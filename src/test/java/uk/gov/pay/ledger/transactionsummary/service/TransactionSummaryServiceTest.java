package uk.gov.pay.ledger.transactionsummary.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transactionsummary.dao.TransactionSummaryDao;
import uk.gov.pay.ledger.util.fixture.EventFixture;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.pay.ledger.event.model.SalientEventType.AUTHORISATION_SUCCEEDED;
import static uk.gov.pay.ledger.event.model.SalientEventType.CAPTURE_CONFIRMED;
import static uk.gov.pay.ledger.event.model.SalientEventType.CAPTURE_ERRORED;
import static uk.gov.pay.ledger.event.model.SalientEventType.PAYMENT_CREATED;
import static uk.gov.pay.ledger.event.model.SalientEventType.PAYMENT_STATUS_CORRECTED_TO_SUCCESS_BY_ADMIN;
import static uk.gov.pay.ledger.event.model.SalientEventType.REFUND_SUBMITTED;
import static uk.gov.pay.ledger.event.model.SalientEventType.SERVICE_APPROVED_FOR_CAPTURE;
import static uk.gov.pay.ledger.event.model.SalientEventType.USER_APPROVED_FOR_CAPTURE;
import static uk.gov.pay.ledger.transaction.state.TransactionState.CREATED;
import static uk.gov.pay.ledger.transaction.state.TransactionState.SUBMITTED;
import static uk.gov.pay.ledger.transaction.state.TransactionState.SUCCESS;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

@ExtendWith(MockitoExtension.class)
public class TransactionSummaryServiceTest {

    private TransactionSummaryService transactionSummaryService;
    @Mock
    TransactionSummaryDao mockTransactionSummaryDao;

    @BeforeEach
    void setUp() {
        transactionSummaryService = new TransactionSummaryService(mockTransactionSummaryDao);
    }

    @Test
    public void shouldProjectTransactionSummaryIfPaymentCreatedEventAndAnEventMappingToFinishedTransactionStateExists() {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withState(SUCCESS)
                .toEntity();
        Event paymentCreatedEvent = EventFixture.anEventFixture()
                .withEventDate(ZonedDateTime.now())
                .withEventType(PAYMENT_CREATED.name()).toEntity();
        Event nonSalientEvent = EventFixture.anEventFixture()
                .withEventDate(ZonedDateTime.now())
                .withEventType("BACKFILLER_RECREATED_USER_EMAIL_COLLECTED").toEntity();
        Event userApprovedForCaptureEvent = EventFixture.anEventFixture()
                .withEventDate(ZonedDateTime.now().plusSeconds(11))
                .withEventType(USER_APPROVED_FOR_CAPTURE.name()).toEntity();
        List<Event> events = List.of(paymentCreatedEvent, nonSalientEvent, userApprovedForCaptureEvent);

        transactionSummaryService.projectTransactionSummary(transactionEntity, paymentCreatedEvent, events);

        verify(mockTransactionSummaryDao).upsert(transactionEntity.getGatewayAccountId(),
                transactionEntity.getTransactionType(), LocalDate.ofInstant(transactionEntity.getCreatedDate().toInstant(), UTC),
                transactionEntity.getState(), transactionEntity.isLive(),
                transactionEntity.isMoto(), transactionEntity.getAmount());
        verifyNoMoreInteractions(mockTransactionSummaryDao);
    }

    @Test
    public void shouldProjectTransactionSummaryIfPaymentNotificationCreatedEventAndAnEventMappingToFinishedTransactionStateExists() {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withState(SUCCESS)
                .toEntity();
        Event paymentCreatedEvent = EventFixture.anEventFixture()
                .withEventDate(ZonedDateTime.now())
                .withEventType("PAYMENT_NOTIFICATION_CREATED").toEntity();
        Event userApprovedForCaptureEvent = EventFixture.anEventFixture()
                .withEventDate(ZonedDateTime.now())
                .withEventType(USER_APPROVED_FOR_CAPTURE.name()).toEntity();

        List<Event> events = List.of(paymentCreatedEvent, userApprovedForCaptureEvent);

        transactionSummaryService.projectTransactionSummary(transactionEntity, userApprovedForCaptureEvent, events);

        verify(mockTransactionSummaryDao).upsert(transactionEntity.getGatewayAccountId(),
                transactionEntity.getTransactionType(), LocalDate.ofInstant(transactionEntity.getCreatedDate().toInstant(), UTC),
                transactionEntity.getState(), transactionEntity.isLive(),
                transactionEntity.isMoto(), transactionEntity.getAmount());
        verifyNoMoreInteractions(mockTransactionSummaryDao);
    }

    @Test
    public void shouldProjectTransactionSummaryIfCurrentEventMapsToDifferentTransactionStateOfPreviousEvent() {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withState(SUCCESS)
                .toEntity();
        Event event = EventFixture.anEventFixture().withEventDate(ZonedDateTime.now())
                .withEventType(PAYMENT_CREATED.name()).toEntity();
        Event event2 = EventFixture.anEventFixture().withEventDate(ZonedDateTime.now().plusSeconds(1))
                .withEventType(USER_APPROVED_FOR_CAPTURE.name()).toEntity();
        Event event3 = EventFixture.anEventFixture().withEventDate(ZonedDateTime.now().plusSeconds(2))
                .withEventType(CAPTURE_ERRORED.name()).toEntity();

        List<Event> events = List.of(event, event2, event3);

        transactionSummaryService.projectTransactionSummary(transactionEntity, event3, events);

        verify(mockTransactionSummaryDao).upsert(transactionEntity.getGatewayAccountId(),
                transactionEntity.getTransactionType(), LocalDate.ofInstant(transactionEntity.getCreatedDate().toInstant(), UTC),
                transactionEntity.getState(), transactionEntity.isLive(),
                transactionEntity.isMoto(), transactionEntity.getAmount());
        verify(mockTransactionSummaryDao).deductTransactionSummaryFor(transactionEntity.getGatewayAccountId(),
                transactionEntity.getTransactionType(), LocalDate.ofInstant(transactionEntity.getCreatedDate().toInstant(), UTC),
                transactionEntity.getState(), transactionEntity.isLive(),
                transactionEntity.isMoto(), transactionEntity.getAmount(), transactionEntity.getFee());
    }

    @Test
    public void shouldProjectTransactionSummaryIfEventIsPaymentCreatedAndNotDeductTransactionSummaryIfMultipleTerminalEventsExists() {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withState(SUCCESS)
                .toEntity();
        Event event = EventFixture.anEventFixture().withEventDate(ZonedDateTime.now())
                .withEventType(PAYMENT_CREATED.name()).toEntity();
        Event event2 = EventFixture.anEventFixture().withEventDate(ZonedDateTime.now().plusSeconds(1))
                .withEventType(USER_APPROVED_FOR_CAPTURE.name()).toEntity();
        Event event3 = EventFixture.anEventFixture().withEventDate(ZonedDateTime.now().plusSeconds(2))
                .withEventType(CAPTURE_ERRORED.name()).toEntity();
        List<Event> events = List.of(event, event2, event3);

        transactionSummaryService.projectTransactionSummary(transactionEntity, event, events);

        verify(mockTransactionSummaryDao).upsert(transactionEntity.getGatewayAccountId(),
                transactionEntity.getTransactionType(), LocalDate.ofInstant(transactionEntity.getCreatedDate().toInstant(), UTC),
                transactionEntity.getState(), transactionEntity.isLive(),
                transactionEntity.isMoto(), transactionEntity.getAmount());
        verifyNoMoreInteractions(mockTransactionSummaryDao);
    }

    @Test
    public void shouldUpdateTransactionSummaryForFeeIfCaptureConfirmedEventExists() {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withState(SUCCESS)
                .withFee(10L)
                .toEntity();
        Event event = EventFixture.anEventFixture().withEventDate(ZonedDateTime.now())
                .withEventType(PAYMENT_CREATED.name()).toEntity();
        Event event2 = EventFixture.anEventFixture().withEventDate(ZonedDateTime.now().plusSeconds(1))
                .withEventType(CAPTURE_CONFIRMED.name()).toEntity();

        List<Event> events = List.of(event, event2);

        transactionSummaryService.projectTransactionSummary(transactionEntity, event, events);

        verify(mockTransactionSummaryDao).upsert(transactionEntity.getGatewayAccountId(),
                transactionEntity.getTransactionType(), LocalDate.ofInstant(transactionEntity.getCreatedDate().toInstant(), UTC),
                transactionEntity.getState(), transactionEntity.isLive(),
                transactionEntity.isMoto(), transactionEntity.getAmount());
        verify(mockTransactionSummaryDao).updateFee(transactionEntity.getGatewayAccountId(),
                transactionEntity.getTransactionType(), LocalDate.ofInstant(transactionEntity.getCreatedDate().toInstant(), UTC),
                transactionEntity.getState(), transactionEntity.isLive(),
                transactionEntity.isMoto(), transactionEntity.getFee());
        verifyNoMoreInteractions(mockTransactionSummaryDao);
    }

    @Test
    public void shouldNotUpdateTransactionSummaryFoFeeIfFeeIsNotAvailableOnTransaction() {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withState(SUCCESS)
                .toEntity();
        Event event = EventFixture.anEventFixture().withEventDate(ZonedDateTime.now())
                .withEventType(PAYMENT_CREATED.name()).toEntity();
        Event event2 = EventFixture.anEventFixture().withEventDate(ZonedDateTime.now().plusSeconds(1))
                .withEventType(CAPTURE_CONFIRMED.name()).toEntity();

        List<Event> events = List.of(event, event2);

        transactionSummaryService.projectTransactionSummary(transactionEntity, event, events);

        verify(mockTransactionSummaryDao).upsert(transactionEntity.getGatewayAccountId(),
                transactionEntity.getTransactionType(), LocalDate.ofInstant(transactionEntity.getCreatedDate().toInstant(), UTC),
                transactionEntity.getState(), transactionEntity.isLive(),
                transactionEntity.isMoto(), transactionEntity.getAmount());

        verifyNoMoreInteractions(mockTransactionSummaryDao);
    }

    @Test
    public void shouldNotUpdateTransactionSummaryForFeeIfMultipleCaptureConfirmedEventsExists() {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withState(SUCCESS)
                .withFee(10L)
                .toEntity();
        Event event = EventFixture.anEventFixture().withEventDate(ZonedDateTime.now())
                .withEventType(PAYMENT_CREATED.name()).toEntity();
        Event event2 = EventFixture.anEventFixture().withEventDate(ZonedDateTime.now().plusSeconds(1))
                .withEventType(CAPTURE_CONFIRMED.name()).toEntity();
        Event event3 = EventFixture.anEventFixture().withEventDate(ZonedDateTime.now().plusSeconds(2))
                .withEventType(CAPTURE_CONFIRMED.name()).toEntity();

        List<Event> events = List.of(event, event2, event3);

        transactionSummaryService.projectTransactionSummary(transactionEntity, event3, events);

        verifyNoInteractions(mockTransactionSummaryDao);
    }

    @Test
    public void shouldNotProjectTransactionSummaryIfCurrentEventMapsToSameTransactionStateAsPreviousEvent() {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withState(SUCCESS)
                .toEntity();
        Event event = EventFixture.anEventFixture().withEventDate(ZonedDateTime.now())
                .withEventType(PAYMENT_CREATED.name()).toEntity();
        Event event2 = EventFixture.anEventFixture()
                .withEventDate(ZonedDateTime.now().plusSeconds(1))
                .withEventType(SERVICE_APPROVED_FOR_CAPTURE.name()).toEntity();
        Event event3 = EventFixture.anEventFixture()
                .withEventDate(ZonedDateTime.now().plusSeconds(2))
                .withEventType(PAYMENT_STATUS_CORRECTED_TO_SUCCESS_BY_ADMIN.name()).toEntity();
        List<Event> events = List.of(event, event2, event3);

        transactionSummaryService.projectTransactionSummary(transactionEntity, event3, events);

        verifyNoInteractions(mockTransactionSummaryDao);
    }

    @Test
    public void shouldNotProjectTransactionSummaryIfOnlyPaymentCreatedEventExists() {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withState(CREATED)
                .toEntity();
        Event event = EventFixture.anEventFixture().withEventType(PAYMENT_CREATED.name()).toEntity();

        transactionSummaryService.projectTransactionSummary(transactionEntity, event, List.of(event));

        verifyNoInteractions(mockTransactionSummaryDao);
    }

    @Test
    public void shouldNotProjectTransactionSummaryIfAnEventMappingToTransactionFinishedStateExistsButPaymentCreatedEventDoNotExists() {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withState(SUCCESS)
                .toEntity();
        Event event = EventFixture.anEventFixture().withEventType(USER_APPROVED_FOR_CAPTURE.name()).toEntity();

        transactionSummaryService.projectTransactionSummary(transactionEntity, event, List.of(event));

        verifyNoInteractions(mockTransactionSummaryDao);
    }

    @Test
    public void shouldNotProjectTransactionSummaryIfEventIsSalientEventButDoesNotMapToTransactionFinishedState() {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withState(SUBMITTED)
                .toEntity();
        Event event = EventFixture.anEventFixture().withEventType(AUTHORISATION_SUCCEEDED.name()).toEntity();

        transactionSummaryService.projectTransactionSummary(transactionEntity, event, List.of(event));

        verifyNoInteractions(mockTransactionSummaryDao);
    }

    @Test
    public void shouldNotProjectTransactionSummaryIfCurrentEventIsNonSalientEventAndTransactionIsFinished() {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withState(SUCCESS)
                .toEntity();
        Event nonSalientEvent = EventFixture.anEventFixture()
                .withEventDate(ZonedDateTime.now())
                .withEventType("BACKFILLER_RECREATED_USER_EMAIL_COLLECTED").toEntity();

        transactionSummaryService.projectTransactionSummary(transactionEntity, nonSalientEvent, List.of(nonSalientEvent));

        verifyNoInteractions(mockTransactionSummaryDao);
    }

    @Test
    public void shouldNotProjectTransactionSummaryIfEventIsNotAPaymentEvent() {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withState(SUBMITTED)
                .toEntity();
        Event event = EventFixture.anEventFixture().withEventType(REFUND_SUBMITTED.name()).toEntity();

        transactionSummaryService.projectTransactionSummary(transactionEntity, event, List.of(event));

        verifyNoInteractions(mockTransactionSummaryDao);
    }
}