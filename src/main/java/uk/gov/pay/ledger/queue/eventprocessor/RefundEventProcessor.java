package uk.gov.pay.ledger.queue.eventprocessor;

import uk.gov.pay.ledger.event.model.Event;
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

public class RefundEventProcessor extends EventProcessor {
    private final EventService eventService;
    private final TransactionService transactionService;
    private final TransactionEntityFactory transactionEntityFactory;

    public RefundEventProcessor(EventService eventService, TransactionService transactionService,
                                TransactionEntityFactory transactionEntityFactory) {

        this.eventService = eventService;
        this.transactionService = transactionService;
        this.transactionEntityFactory = transactionEntityFactory;
    }

    @Override
    public void process(Event event, boolean isANewEvent) {
        EventDigest refundEventDigest = eventService.getEventDigestForResource(event);
        Optional<EventDigest> mayBePaymentEventDigest = Optional.empty();

        if (isNotBlank(refundEventDigest.getParentResourceExternalId())) {
            mayBePaymentEventDigest = getPaymentEventDigest(refundEventDigest.getParentResourceExternalId());
        }

        mayBePaymentEventDigest.ifPresentOrElse(
                paymentEventDigest -> projectRefundTransactionWithPaymentDetails(refundEventDigest, paymentEventDigest),
                () -> transactionService.upsertTransactionFor(refundEventDigest));
    }

    public void reprojectRefundTransaction(String refundExternalId, EventDigest paymentEventDigest) {
        EventDigest refundEventDigest = eventService.getEventDigestForResource(refundExternalId);
        projectRefundTransactionWithPaymentDetails(refundEventDigest, paymentEventDigest);
    }

    private void projectRefundTransactionWithPaymentDetails(EventDigest refundEventDigest, EventDigest paymentEventDigest) {
        /**
         * Apply shared refund payment attributes to the refund digest
         *
         * Frontend consumers rely on searching/ filtering/ downloading attributes that belong to a payment on the
         * refund. Previously this was done at the "view" level by joining transactions to transactions, for performance
         * reasons this is now done here during domain object projection (as transactions are de-normalised).
         *
         * If there is no longer a frontend requirement to display payment information on a refund, this shared data
         * for the digest can be removed.
         */
        Map<String, Object> fieldsFromPayment = getPaymentFieldsToProjectOnToRefund(paymentEventDigest);
        refundEventDigest.getEventAggregate().put("payment_details", fieldsFromPayment);

        TransactionEntity refundTransactionEntity = transactionEntityFactory.create(refundEventDigest);
        TransactionEntity paymentTransactionEntity = transactionEntityFactory.create(paymentEventDigest);
        refundTransactionEntity.setEntityFieldsFromOriginalPayment(paymentTransactionEntity);

        transactionService.upsertTransaction(refundTransactionEntity);
    }

    private Optional<EventDigest> getPaymentEventDigest(String paymentExternalId) {
        EventDigest paymentEventDigest = null;
        try {
            paymentEventDigest = eventService.getEventDigestForResource(paymentExternalId);
        } catch (EmptyEventsException ignored) {
            // no valid refund projection is possible without payment events, allow upstream to handle this
        }
        return Optional.ofNullable(paymentEventDigest);
    }

    private Map<String, Object> getPaymentFieldsToProjectOnToRefund(EventDigest paymentEventDigest) {
        List<String> paymentsFieldsToCopyToRefunds = List.of("card_brand_label", "expiry_date", "card_type", "wallet_type");

        var paymentPayloadIsEmpty = paymentEventDigest == null || paymentEventDigest.getEventAggregate() == null;

        return paymentPayloadIsEmpty
                ? Map.of()
                : paymentEventDigest.getEventAggregate()
                .entrySet()
                .stream().filter(entry -> paymentsFieldsToCopyToRefunds.contains(entry.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
