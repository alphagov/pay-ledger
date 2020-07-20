package uk.gov.pay.ledger.queue.eventprocessor;

import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.exception.EmptyEventsException;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void process(Event event) {
        EventDigest refundEventDigest = eventService.getEventDigestForResource(event);

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
        if (isNotBlank(refundEventDigest.getParentResourceExternalId())) {
            Map<String, Object> fieldsFromPayment = getFieldsFromOriginalPayment(refundEventDigest.getParentResourceExternalId());
            refundEventDigest.getEventPayload().putAll(fieldsFromPayment);
        }

        transactionService.upsertTransactionFor(refundEventDigest);
    }

    private Map<String, Object> getFieldsFromOriginalPayment(String paymentExternalId) {
        EventDigest paymentEventDigest = null;
        List<String> paymentsFieldsToCopyToRefunds = List.of("cardholder_name", "email", "description",
                "card_brand", "last_digits_card_number", "first_digits_card_number", "reference",
                "card_brand_label", "expiry_date", "card_type", "wallet_type");

        try {
            paymentEventDigest = eventService.getEventDigestForResource(paymentExternalId);
        } catch (EmptyEventsException ignored) {
            // no valid refund projection is possible without payment events, allow upstream to handle this
        }

        var paymentPayloadIsEmpty = paymentEventDigest == null || paymentEventDigest.getEventPayload() == null;

        return paymentPayloadIsEmpty
                ? Map.of()
                : paymentEventDigest.getEventPayload()
                    .entrySet()
                    .stream().filter(entry -> paymentsFieldsToCopyToRefunds.contains(entry.getKey()))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
