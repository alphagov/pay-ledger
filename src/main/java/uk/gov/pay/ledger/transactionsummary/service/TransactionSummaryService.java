package uk.gov.pay.ledger.transactionsummary.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.transactionsummary.dao.TransactionSummaryDao;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;
import static uk.gov.pay.ledger.event.model.SalientEventType.CAPTURE_CONFIRMED;
import static uk.gov.pay.ledger.event.model.SalientEventType.PAYMENT_CREATED;
import static uk.gov.pay.ledger.event.model.SalientEventType.QUEUED_FOR_CAPTURE;
import static uk.gov.pay.ledger.event.model.SalientEventType.SERVICE_APPROVED_FOR_CAPTURE;
import static uk.gov.pay.ledger.event.model.SalientEventType.USER_APPROVED_FOR_CAPTURE;
import static uk.gov.pay.ledger.event.model.SalientEventType.from;

public class TransactionSummaryService {

    private final TransactionSummaryDao transactionSummaryDao;

    // CAPTURE_SUBMITTED event (common to payment notifications, service approved and user approved payments) will be
    // considered to project transaction summary for success state. Ignore the following events that result into the
    // same success state and to avoid counting the transaction multiple times
    private final List<String> ignoreEventsForTransactionAmount = List.of(CAPTURE_CONFIRMED.name(),
            QUEUED_FOR_CAPTURE.name(),
            USER_APPROVED_FOR_CAPTURE.name(),
            SERVICE_APPROVED_FOR_CAPTURE.name());

    @Inject
    public TransactionSummaryService(TransactionSummaryDao transactionSummaryDao) {
        this.transactionSummaryDao = transactionSummaryDao;
    }

    public void projectTransactionSummary(TransactionEntity transaction, Event currentEvent, List<Event> events) {
        if (TransactionType.PAYMENT.name().equals(transaction.getTransactionType())) {
            projectPaymentTransactionSummary(transaction, currentEvent, events);
        }
    }

    private void projectPaymentTransactionSummary(TransactionEntity transaction, Event currentEvent,
                                                  List<Event> events) {
        if (canProjectTransactionAmount(currentEvent, events)) {
            projectTransactionAmount(transaction, currentEvent, events);
        }

        if (canProjectTransactionFee(transaction, currentEvent, events)) {
            projectTransactionFee(transaction);
        }
    }

    private boolean canProjectTransactionAmount(Event currentEvent, List<Event> events) {
        if (!hasPaymentCreatedOrNotificationEvent(events)
                || ignoreEventsForTransactionAmount.contains(currentEvent.getEventType())) {
            return false;
        }

        if (isAPaymentOrNotificationCreatedEvent(currentEvent)
                && !getEventsMappingToTransactionFinishedStateInDescendingOrder(events).isEmpty()) {
            return true;
        }

        return getTransactionState(currentEvent).map(TransactionState::isFinished).orElse(false);
    }

    private boolean canProjectTransactionFee(TransactionEntity transaction, Event currentEvent, List<Event> events) {
        Optional<SalientEventType> mayBeCurrentSalientEventType = from(currentEvent.getEventType());
        if (mayBeCurrentSalientEventType.isPresent()) {
            long noOfCaptureConfirmedEvents = events
                    .stream()
                    .map(event -> from(event.getEventType()))
                    .flatMap(Optional::stream)
                    .filter(salientEventType -> salientEventType == CAPTURE_CONFIRMED)
                    .count();

            // Fee is currently immutable, so process fee only for the first capture_confirmed event seen
            // or if payment_created is processed after a capture_confirmed event has been processed.
            return (mayBeCurrentSalientEventType.get() == PAYMENT_CREATED ||
                    (mayBeCurrentSalientEventType.get() == CAPTURE_CONFIRMED && noOfCaptureConfirmedEvents == 1))
                    && transaction.getFee() != null;
        }
        return false;
    }

    private boolean isAPaymentOrNotificationCreatedEvent(Event event) {
        return from(event.getEventType()).orElse(null) == PAYMENT_CREATED
                || "PAYMENT_NOTIFICATION_CREATED".equals(event.getEventType());
    }

    private void projectTransactionAmount(TransactionEntity transaction, Event currentEvent,
                                          List<Event> events) {
        List<Event> eventsMappingToFinishedState =
                getEventsMappingToTransactionFinishedStateInDescendingOrder(events);

        if (eventsMappingToFinishedState.size() > 1
                && PAYMENT_CREATED != from(currentEvent.getEventType()).orElse(null)) {
            Event previousEvent = eventsMappingToFinishedState.get(1);

            if (currentEventTransactionStateMatchesWithPreviousEvent(currentEvent, previousEvent)) {
                return;
            }

            transactionSummaryDao.deductTransactionSummaryFor(transaction.getGatewayAccountId(),
                    transaction.getTransactionType(), toLocalDate(transaction.getCreatedDate()),
                    getTransactionState(previousEvent).get(), transaction.isLive(), transaction.isMoto(),
                    (transaction.getTotalAmount() != null ? transaction.getTotalAmount() : transaction.getAmount()),
                    transaction.getFee());
        }
        transactionSummaryDao.upsert(transaction.getGatewayAccountId(), transaction.getTransactionType(),
                toLocalDate(transaction.getCreatedDate()), transaction.getState(),
                transaction.isLive(), transaction.isMoto(),
                (transaction.getTotalAmount() != null ? transaction.getTotalAmount() : transaction.getAmount()));
    }

    private void projectTransactionFee(TransactionEntity transaction) {
        transactionSummaryDao.updateFee(transaction.getGatewayAccountId(), transaction.getTransactionType(),
                toLocalDate(transaction.getCreatedDate()), transaction.getState(), transaction.isLive(),
                transaction.isMoto(), transaction.getFee());
    }

    private LocalDate toLocalDate(ZonedDateTime createdDate) {
        return LocalDate.ofInstant(createdDate.toInstant(), UTC);
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
        return events.stream()
                .filter(event -> !ignoreEventsForTransactionAmount.contains(event.getEventType()))
                .filter(event -> getTransactionState(event).map(TransactionState::isFinished).orElse(false))
                .sorted(Comparator.comparing(Event::getEventDate).reversed())
                .collect(Collectors.toList());
    }

    private boolean hasPaymentCreatedOrNotificationEvent(List<Event> events) {
        return events.stream()
                .anyMatch(this::isAPaymentOrNotificationCreatedEvent);
    }
}
