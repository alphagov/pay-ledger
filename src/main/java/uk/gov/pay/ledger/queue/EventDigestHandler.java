package uk.gov.pay.ledger.queue;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.payout.service.PayoutService;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.service.TransactionMetadataService;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class EventDigestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDigestHandler.class);

    private final EventService eventService;
    private final TransactionService transactionService;
    private final TransactionMetadataService transactionMetadataService;
    private final PayoutService payoutService;
    private TransactionEntityFactory transactionEntityFactory;

    @Inject
    public EventDigestHandler(EventService eventService,
                              TransactionService transactionService,
                              TransactionMetadataService transactionMetadataService,
                              PayoutService payoutService,
                              TransactionEntityFactory transactionEntityFactory) {
        this.eventService = eventService;
        this.transactionService = transactionService;
        this.transactionMetadataService = transactionMetadataService;
        this.payoutService = payoutService;
        this.transactionEntityFactory = transactionEntityFactory;
    }

    public EventProcessor processorFor(Event event) {
        switch (event.getResourceType()) {
            case PAYMENT:
                return new PaymentEventProcessor();
            case REFUND:
                return new RefundEventProcessor();
            case PAYOUT:
                return new PayoutEventProcessor();
            default:
                String message = String.format("Event digest processing for resource type [%s] is not supported. Event type [%s] and resource external id [%s]",
                        event.getResourceType(),
                        event.getEventType(),
                        event.getResourceExternalId());
                LOGGER.error(message);
                throw new RuntimeException(message);
        }

    }

    public void processEvent(Event event) {
        processorFor(event).process(event);
    }

    static abstract class EventProcessor {
        public abstract void process(Event event);

    }

    class PaymentEventProcessor extends EventProcessor {
        @Override
        public void process(Event event) {

            EventDigest eventDigest = eventService.getEventDigestForResource(event);
            transactionService.upsertTransactionFor(eventDigest);
            transactionMetadataService.upsertMetadataFor(event);
        }
    }

    class RefundEventProcessor extends EventProcessor {

        @Override
        public void process(Event event) {
            EventDigest refundEventDigest = eventService.getEventDigestForResource(event);

            Map<String, Object> fieldsFromPayment = getFieldsFromOriginalPayment(refundEventDigest.getParentResourceExternalId());

            Map<String, Object> refundEventPayload = new HashMap<>(refundEventDigest.getEventPayload());
            refundEventPayload.putAll(fieldsFromPayment);

            TransactionEntity refund = transactionEntityFactory.create(refundEventDigest, refundEventPayload);

            transactionService.upsertTransaction(refund);
            transactionMetadataService.upsertMetadataFor(event);
        }

        private Map<String, Object> getFieldsFromOriginalPayment(String paymentExternalId) {
            List<String> paymentsFieldsToCopyToRefunds = List.of("cardholder_name", "email", "description",
                    "card_brand", "last_digits_card_number", "first_digits_card_number", "reference",
                    "card_brand_label", "expiry_date", "card_type", "wallet_type");

            EventDigest paymentEventDigest = eventService.getEventDigestForResource(paymentExternalId);

            return paymentEventDigest == null || paymentEventDigest.getEventPayload() == null ?
                    Map.of()
                    : paymentEventDigest.getEventPayload()
                    .entrySet()
                    .stream().filter(entry -> paymentsFieldsToCopyToRefunds.contains(entry.getKey()))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    class PayoutEventProcessor extends EventProcessor {
        @Override
        public void process(Event event) {
            payoutService.upsertPayoutFor(eventService.getEventDigestForResource(event));
        }
    }

}
