package uk.gov.pay.ledger.queue.eventprocessor;

import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.event.service.EventService;
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

        Map<String, Object> refundEventPayload = new HashMap<>(refundEventDigest.getEventPayload());

        if (isNotBlank(refundEventDigest.getParentResourceExternalId())) {
            Map<String, Object> fieldsFromPayment = getFieldsFromOriginalPayment(
                    refundEventDigest.getParentResourceExternalId());
            refundEventPayload.putAll(fieldsFromPayment);
        }

        TransactionEntity refund = transactionEntityFactory.create(refundEventDigest, refundEventPayload);

        transactionService.upsertTransaction(refund);
    }

    private Map<String, Object> getFieldsFromOriginalPayment(String paymentExternalId) {
        List<String> paymentsFieldsToCopyToRefunds = List.of("cardholder_name", "email", "description",
                "card_brand", "last_digits_card_number", "first_digits_card_number", "reference",
                "card_brand_label", "expiry_date", "card_type", "wallet_type");

        // todo : catch 'no events found' exception ? Pacts fail currently
        EventDigest paymentEventDigest = eventService.getEventDigestForResource(paymentExternalId);

        return paymentEventDigest == null || paymentEventDigest.getEventPayload() == null ? Map.of()
                : paymentEventDigest.getEventPayload()
                .entrySet()
                .stream().filter(entry -> paymentsFieldsToCopyToRefunds.contains(entry.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
