package uk.gov.pay.ledger.queue.eventprocessor;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.dao.entity.EventEntity;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.exception.EmptyEventsException;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ChildTransactionEventProcessor extends EventProcessor {

    private final EventService eventService;
    private final TransactionService transactionService;
    private final TransactionEntityFactory transactionEntityFactory;

    @Inject
    public ChildTransactionEventProcessor(EventService eventService,
                                          TransactionService transactionService,
                                          TransactionEntityFactory transactionEntityFactory) {

        this.eventService = eventService;
        this.transactionService = transactionService;
        this.transactionEntityFactory = transactionEntityFactory;
    }

    @Override
    public void process(EventEntity event, boolean isANewEvent) {
        EventDigest childTransactionEventDigest = eventService.getEventDigestForResource(event);

        Optional<EventDigest> mayBePaymentEventDigest = Optional.empty();

        if (isNotBlank(childTransactionEventDigest.getParentResourceExternalId())) {
            mayBePaymentEventDigest = getPaymentEventDigest(childTransactionEventDigest.getParentResourceExternalId());
        }

        mayBePaymentEventDigest.ifPresentOrElse(
                paymentEventDigest -> projectChildTransactionWithPaymentDetails(childTransactionEventDigest, paymentEventDigest),
                () -> transactionService.upsertTransactionFor(childTransactionEventDigest));
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
