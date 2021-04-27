package uk.gov.pay.ledger.transactionsummary.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.transactionsummary.dao.TransactionSummaryDao;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.pay.ledger.event.model.SalientEventType.CAPTURE_CONFIRMED;
import static uk.gov.pay.ledger.event.model.SalientEventType.PAYMENT_CREATED;
import static uk.gov.pay.ledger.event.model.SalientEventType.from;
import static uk.gov.pay.ledger.transaction.model.TransactionType.PAYMENT;

public class TransactionSummaryService {

    private final TransactionSummaryDao transactionSummaryDao;
    private final EventService eventService;

    @Inject
    public TransactionSummaryService(TransactionSummaryDao transactionSummaryDao, EventService eventService) {
        this.transactionSummaryDao = transactionSummaryDao;
        this.eventService = eventService;
    }

    public void projectTransactionSummary(TransactionEntity transaction, Event currentEvent) {
        if (PAYMENT.name().equals(transaction.getTransactionType())) {
            projectPaymentTransactionSummary(transaction, currentEvent);
        }
    }

    private void projectPaymentTransactionSummary(TransactionEntity transaction, Event currentEvent) {

        if (!canProjectTransactionSummary(transaction, currentEvent)) {
            return;
        }

        List<Event> events = eventService.getEventsForResource(transaction.getExternalId());
        boolean hasPaymentCreatedEvent = hasSalientEvent(events, PAYMENT_CREATED);

        if (!hasPaymentCreatedEvent) {
            return;
        }

        List<Event> eventsMappingToFinishedState =
                getEventsMappingToTransactionFinishedStateInDescendingOrder(events);
        Optional<SalientEventType> mayBeCurrentSalientEventType = from(currentEvent.getEventType());

        projectTransactionAmount(transaction, currentEvent, eventsMappingToFinishedState);
        projectTransactionFee(transaction, mayBeCurrentSalientEventType.get(), eventsMappingToFinishedState);
    }

    private boolean canProjectTransactionSummary(TransactionEntity transaction, Event currentEvent) {
        SalientEventType currentSalientEventType = from(currentEvent.getEventType()).orElse(null);

        if (currentSalientEventType == PAYMENT_CREATED && transaction.getState().isFinished()) {
            return true;
        }

        return getTransactionState(currentEvent).map(TransactionState::isFinished).orElse(false);
    }

    private void projectTransactionAmount(TransactionEntity transaction, Event currentEvent,
                                          List<Event> eventsMappingToTransactionFinishedState) {
        if (eventsMappingToTransactionFinishedState.size() > 1
                && PAYMENT_CREATED != from(currentEvent.getEventType()).orElse(null)) {
            Event previousEvent = eventsMappingToTransactionFinishedState.get(1);

            if (currentEventTransactionStateMatchesWithPreviousEvent(currentEvent, previousEvent)) {
                return;
            }

            transactionSummaryDao.deductTransactionSummaryFor(transaction.getGatewayAccountId(),
                    transaction.getTransactionType(), transaction.getCreatedDate(),
                    getTransactionState(previousEvent).get(), transaction.isLive(), transaction.isMoto(),
                    (transaction.getTotalAmount() != null ? transaction.getTotalAmount() : transaction.getAmount()),
                    transaction.getFee());
        }
        transactionSummaryDao.upsert(transaction.getGatewayAccountId(), transaction.getTransactionType(),
                transaction.getCreatedDate(), transaction.getState(), transaction.isLive(), transaction.isMoto(),
                (transaction.getTotalAmount() != null ? transaction.getTotalAmount() : transaction.getAmount()));
    }

    private void projectTransactionFee(TransactionEntity transaction, SalientEventType currentEvent,
                                       List<Event> eventsMappingToFinishedState) {

        long noOfCaptureConfirmedEvents = eventsMappingToFinishedState
                .stream()
                .map(event -> from(event.getEventType()))
                .flatMap(Optional::stream)
                .filter(salientEventType -> salientEventType == CAPTURE_CONFIRMED)
                .count();

        // Fee is currently immutable, so process fee only for the first capture_confirmed event seen
        // or if payment_created is processed after a capture_confirmed event has been processed.
        if ((currentEvent == PAYMENT_CREATED || (currentEvent == CAPTURE_CONFIRMED && noOfCaptureConfirmedEvents == 1))
                && transaction.getFee() != null) {
            transactionSummaryDao.updateFee(transaction.getGatewayAccountId(), transaction.getTransactionType(),
                    transaction.getCreatedDate(), transaction.getState(), transaction.isLive(), transaction.isMoto(),
                    transaction.getFee());
        }
    }

    private boolean currentEventTransactionStateMatchesWithPreviousEvent(Event currentEvent, Event previousEvent) {
        Optional<TransactionState> previousEventTransactionState = getTransactionState(previousEvent);
        Optional<TransactionState> currentEventTransactionState = getTransactionState(currentEvent);

        return previousEventTransactionState.equals(currentEventTransactionState);
    }

    private Optional<TransactionState> getTransactionState(Event previousEvent) {
        return from(previousEvent.getEventType())
                .map(TransactionState::fromEventType);
    }

    private List<Event> getEventsMappingToTransactionFinishedStateInDescendingOrder(List<Event> events) {
        return events.stream().
                filter(event -> getTransactionState(event).map(TransactionState::isFinished).orElse(false))
                .sorted(Comparator.comparing(Event::getEventDate).reversed())
                .collect(Collectors.toList());
    }

    private boolean hasSalientEvent(List<Event> events, SalientEventType eventToCheck) {
        return events.stream()
                .map(event -> from(event.getEventType()))
                .flatMap(Optional::stream)
                .anyMatch(salientEventType -> eventToCheck == salientEventType);
    }
}
