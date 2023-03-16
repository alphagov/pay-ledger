package uk.gov.pay.ledger.queue.eventprocessor;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.entity.EventEntity;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.service.TransactionMetadataService;
import uk.gov.pay.ledger.transaction.service.TransactionService;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.transactionsummary.service.TransactionSummaryService;
import uk.gov.pay.ledger.util.JsonParser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.pay.ledger.transaction.state.TransactionState.fromEventType;

public class PaymentEventProcessor extends EventProcessor {

    private final EventService eventService;
    private final TransactionService transactionService;
    private final TransactionMetadataService transactionMetadataService;
    private final ChildTransactionEventProcessor childTransactionEventProcessor;
    private final TransactionSummaryService transactionSummaryService;

    @Inject
    public PaymentEventProcessor(EventService eventService,
                                 TransactionService transactionService,
                                 TransactionMetadataService transactionMetadataService,
                                 ChildTransactionEventProcessor childTransactionEventProcessor,
                                 TransactionSummaryService transactionSummaryService) {
        this.eventService = eventService;
        this.transactionService = transactionService;
        this.transactionMetadataService = transactionMetadataService;
        this.childTransactionEventProcessor = childTransactionEventProcessor;
        this.transactionSummaryService = transactionSummaryService;
    }


    @Override
    public void process(EventEntity event, boolean isANewEvent) {
        List<EventEntity> events = eventService.getEventsForResource(event.getResourceExternalId());
        EventDigest paymentEventDigest = EventDigest.fromEventList(events);

        TransactionEntity transactionEntity = transactionService.upsertTransactionFor(paymentEventDigest);

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
                    .forEach(refundTransactionEntity -> childTransactionEventProcessor.reprojectChildTransaction(refundTransactionEntity.getExternalId(), paymentEventDigest));
        }

        if (!event.isReprojectDomainObject() && isANewEvent) {
            transactionSummaryService.projectTransactionSummary(transactionEntity, event, events);
        }
    }

    private boolean hasSuccessEvent(List<EventEntity> events) {
        return events.stream().map(event -> SalientEventType.from(event.getEventType()))
                .flatMap(Optional::stream)
                .anyMatch(salientEventType -> fromEventType(salientEventType) == TransactionState.SUCCESS);
    }
}
