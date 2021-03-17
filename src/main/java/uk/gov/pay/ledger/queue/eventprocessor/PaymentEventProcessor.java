package uk.gov.pay.ledger.queue.eventprocessor;

import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.transaction.service.TransactionMetadataService;
import uk.gov.pay.ledger.transaction.service.TransactionService;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.JsonParser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PaymentEventProcessor extends EventProcessor {

    private EventService eventService;
    private TransactionService transactionService;
    private TransactionMetadataService transactionMetadataService;
    private RefundEventProcessor refundEventProcessor;

    public PaymentEventProcessor(EventService eventService,
                                 TransactionService transactionService,
                                 TransactionMetadataService transactionMetadataService,
                                 RefundEventProcessor refundEventProcessor) {

        this.eventService = eventService;
        this.transactionService = transactionService;
        this.transactionMetadataService = transactionMetadataService;
        this.refundEventProcessor = refundEventProcessor;
    }

    @Override
    public void process(Event event) {
        List<Event> events = eventService.getEventsForResource(event.getResourceExternalId());
        EventDigest paymentEventDigest = EventDigest.fromEventList(events);

        transactionService.upsertTransactionFor(paymentEventDigest);

        if (event.isReprojectDomainObject()) {
            transactionMetadataService.reprojectFromEventDigest(paymentEventDigest);
        } else {
            transactionMetadataService.upsertMetadataFor(event);
        }

        /**
         * If the payment has associated refunds, we want to update the payment details that we also store on refunds to
         * keep these in sync with the payment.
         * We avoid a database query to get refunds when the payment has not been in a success state, as it is not
         * possible for refunds to exist in this case. We also avoid this query when the current event contains no data
         * that needs to be updated on the refund.
         */
        Map<String, Object> eventDataMap = JsonParser.jsonStringToMap(event.getEventData());
        boolean shouldCheckForRefundsToUpdate = !event.getEventType().equals("REFUND_AVAILABILITY_UPDATED") &&
                !(eventDataMap == null || eventDataMap.isEmpty()) &&
                hasSuccessEvent(events);

        if (shouldCheckForRefundsToUpdate) {
            transactionService.getChildTransactions(event.getResourceExternalId())
                    .forEach(refundTransactionEntity -> refundEventProcessor.reprojectRefundTransaction(refundTransactionEntity.getExternalId(), paymentEventDigest));
        }
    }

    private boolean hasSuccessEvent(List<Event> events) {
        return events.stream().map(event -> SalientEventType.from(event.getEventType()))
                .flatMap(Optional::stream)
                .anyMatch(salientEventType -> TransactionState.fromEventType(salientEventType) == TransactionState.SUCCESS);
    }
}
