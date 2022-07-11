package uk.gov.pay.ledger.queue.eventprocessor;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.exception.EmptyEventsException;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ChildTransactionEventProcessor extends EventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChildTransactionEventProcessor.class);

    private final EventService eventService;
    private final TransactionService transactionService;
    private final TransactionEntityFactory transactionEntityFactory;
    private final LedgerConfig ledgerConfig;
    private final Clock clock;

    @Inject
    public ChildTransactionEventProcessor(EventService eventService,
                                          TransactionService transactionService,
                                          TransactionEntityFactory transactionEntityFactory,
                                          LedgerConfig ledgerConfig,
                                          Clock clock) {

        this.eventService = eventService;
        this.transactionService = transactionService;
        this.transactionEntityFactory = transactionEntityFactory;
        this.ledgerConfig = ledgerConfig;
        this.clock = clock;
    }

    @Override
    public void process(Event event, boolean isANewEvent) {
        EventDigest childTransactionEventDigest = eventService.getEventDigestForResource(event);
        if (event.getResourceType() == ResourceType.DISPUTE && !shouldProjectDisputeTransaction(event, childTransactionEventDigest)) {
            return;
        }

        Optional<EventDigest> mayBePaymentEventDigest = Optional.empty();

        if (isNotBlank(childTransactionEventDigest.getParentResourceExternalId())) {
            mayBePaymentEventDigest = getPaymentEventDigest(childTransactionEventDigest.getParentResourceExternalId());
        }

        mayBePaymentEventDigest.ifPresentOrElse(
                paymentEventDigest -> projectChildTransactionWithPaymentDetails(childTransactionEventDigest, paymentEventDigest),
                () -> transactionService.upsertTransactionFor(childTransactionEventDigest));
    }

    private boolean shouldProjectDisputeTransaction(Event event, EventDigest childTransactionEventDigest) {
        Instant now = clock.instant();
        if (Boolean.TRUE.equals(childTransactionEventDigest.isLive())) {
            if (now.isBefore(ledgerConfig.getQueueMessageReceiverConfig().getProjectLivePaymentsDisputeEventsFromDate())) {
                LOGGER.info("Projecting disputes is not enabled for live transactions. Event type {} and resource external id {}",
                        event.getEventType(),
                        event.getResourceExternalId());
                return false;
            }
        } else {
            if (now.isBefore(ledgerConfig.getQueueMessageReceiverConfig().getProjectTestPaymentsDisputeEventsFromDate())) {
                LOGGER.info("Projecting disputes is not enabled for test transactions. Event type {} and resource external id {}",
                        event.getEventType(),
                        event.getResourceExternalId());
                return false;
            }
        }
        return true;
    }

    public void reprojectChildTransaction(String childTransactionExternalId, EventDigest paymentEventDigest) {
        EventDigest childTransactionEventDigest = eventService.getEventDigestForResource(childTransactionExternalId);
        projectChildTransactionWithPaymentDetails(childTransactionEventDigest, paymentEventDigest);
    }

    private void projectChildTransactionWithPaymentDetails(EventDigest childTransactionEventDigest, EventDigest paymentEventDigest) {
        /**
         * Apply shared payment attributes to the refund/dispute digest
         *
         * Frontend consumers rely on searching/ filtering/ downloading attributes that belong to a payment on the
         * refund/dispute. Previously this was done at the "view" level by joining transactions to transactions, for performance
         * reasons this is now done here during domain object projection (as transactions are de-normalised).
         *
         * If there is no longer a frontend requirement to display payment information on a refund/dispute, this shared data
         * for the digest can be removed.
         */
        Map<String, Object> fieldsFromPayment = getPaymentFieldsToProjectOnToChildTransaction(paymentEventDigest);
        childTransactionEventDigest.getEventAggregate().put("payment_details", fieldsFromPayment);

        TransactionEntity childTransactionEntity = transactionEntityFactory.create(childTransactionEventDigest);
        TransactionEntity paymentTransactionEntity = transactionEntityFactory.create(paymentEventDigest);
        childTransactionEntity.setEntityFieldsFromOriginalPayment(paymentTransactionEntity);

        transactionService.upsertTransaction(childTransactionEntity);
    }

    private Optional<EventDigest> getPaymentEventDigest(String paymentExternalId) {
        EventDigest paymentEventDigest = null;
        try {
            paymentEventDigest = eventService.getEventDigestForResource(paymentExternalId);
        } catch (EmptyEventsException ignored) {
            // no valid refund/digest projection is possible without payment events, allow upstream to handle this
        }
        return Optional.ofNullable(paymentEventDigest);
    }

    private Map<String, Object> getPaymentFieldsToProjectOnToChildTransaction(EventDigest paymentEventDigest) {
        List<String> paymentsFieldsToCopyToChildTransaction = List.of("card_brand_label", "expiry_date", "card_type", "wallet_type");

        var paymentPayloadIsEmpty = paymentEventDigest == null || paymentEventDigest.getEventAggregate() == null;

        return paymentPayloadIsEmpty
                ? Map.of()
                : paymentEventDigest.getEventAggregate()
                .entrySet()
                .stream().filter(entry -> paymentsFieldsToCopyToChildTransaction.contains(entry.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
