package uk.gov.pay.ledger.queue.eventprocessor;

import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.service.TransactionMetadataService;
import uk.gov.pay.ledger.transaction.service.TransactionService;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.transactionsummary.dao.TransactionSummaryDao;
import uk.gov.pay.ledger.util.JsonParser;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.pay.ledger.transaction.state.TransactionState.fromEventType;

public class PaymentEventProcessor extends EventProcessor {

    private EventService eventService;
    private TransactionService transactionService;
    private TransactionMetadataService transactionMetadataService;
    private RefundEventProcessor refundEventProcessor;
    private TransactionSummaryDao transactionSummaryDao;

    public PaymentEventProcessor(EventService eventService,
                                 TransactionService transactionService,
                                 TransactionMetadataService transactionMetadataService,
                                 RefundEventProcessor refundEventProcessor,
                                 TransactionSummaryDao transactionSummaryDao) {
        this.eventService = eventService;
        this.transactionService = transactionService;
        this.transactionMetadataService = transactionMetadataService;
        this.refundEventProcessor = refundEventProcessor;
        this.transactionSummaryDao = transactionSummaryDao;
    }

    @Override
    public void process(Event event) {
        List<Event> events = eventService.getEventsForResource(event.getResourceExternalId());
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
                    .forEach(refundTransactionEntity -> refundEventProcessor.reprojectRefundTransaction(refundTransactionEntity.getExternalId(), paymentEventDigest));
        }

        if (!event.isReprojectDomainObject()) {
            projectTransactionSummary(transactionEntity, events, event);
        }
    }

    private void projectTransactionSummary(TransactionEntity transactionEntity, List<Event> events, Event currentEvent) {

        Optional<TransactionState> mayBeTransactionEventState = SalientEventType.from(currentEvent.getEventType())
                .map(TransactionState::fromEventType);

        mayBeTransactionEventState.ifPresent(transactionState -> {
            // todo:
            // 1. check if created exists for payment transaction. If event is created, check if any previous event has terminal state
            // 2. separate for refund. need refunds now ?
            // 3. scenario for multiple terminal events
            // 4. handle out of order events - PAYMENT_CREATED and then terminal state , terminal state and then PAYMENT_CREATED event
            if (transactionState.isFinished()) {
                List<Event> finishedEvents = events.stream().
                        filter(event1 -> SalientEventType.from(event1.getEventType())
                                .map(TransactionState::fromEventType)
                                .map(TransactionState::isFinished)
                                .orElse(false))
                        .sorted(Comparator.comparing(Event::getEventDate).reversed())
                        .collect(Collectors.toList());

                if (finishedEvents.size() == 1) {
                    transactionSummaryDao.upsert(transactionEntity.getGatewayAccountId(),
                            transactionEntity.getCreatedDate(),
                            transactionEntity.getState(),
                            transactionEntity.isLive(),
                            transactionEntity.getTotalAmount() != null ? transactionEntity.getTotalAmount() : transactionEntity.getAmount());
                } else {
                    Event previousEvent = finishedEvents.get(1);
                    TransactionState previousEventState = SalientEventType.from(previousEvent.getEventType())
                            .map(TransactionState::fromEventType).get();

                    if (!previousEventState.getStatus().equals(transactionState.getStatus())) {
                        transactionSummaryDao.upsert(transactionEntity.getGatewayAccountId(),
                                transactionEntity.getCreatedDate(),
                                transactionEntity.getState(),
                                transactionEntity.isLive(),
                                transactionEntity.getTotalAmount() != null ? transactionEntity.getTotalAmount() : transactionEntity.getAmount());
                        transactionSummaryDao.updateSummary(transactionEntity.getGatewayAccountId(),
                                transactionEntity.getCreatedDate(),
                                previousEventState,
                                transactionEntity.isLive(),
                                transactionEntity.getTotalAmount() != null ? transactionEntity.getTotalAmount() : transactionEntity.getAmount());
                    }
                }
            }
        });
    }

    private boolean hasSuccessEvent(List<Event> events) {
        return events.stream().map(event -> SalientEventType.from(event.getEventType()))
                .flatMap(Optional::stream)
                .anyMatch(salientEventType -> fromEventType(salientEventType) == TransactionState.SUCCESS);
    }
}
