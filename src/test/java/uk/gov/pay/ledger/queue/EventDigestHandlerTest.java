package uk.gov.pay.ledger.queue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.payout.service.PayoutService;
import uk.gov.pay.ledger.transaction.service.TransactionMetadataService;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import java.util.List;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.pay.ledger.event.model.ResourceType.AGREEMENT;
import static uk.gov.pay.ledger.event.model.ResourceType.PAYMENT;
import static uk.gov.pay.ledger.event.model.ResourceType.PAYOUT;
import static uk.gov.pay.ledger.event.model.ResourceType.REFUND;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;

@ExtendWith(MockitoExtension.class)
public class EventDigestHandlerTest {

    @Mock
    private EventService eventService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private TransactionMetadataService transactionMetadataService;
    @Mock
    private PayoutService payoutService;
    private TransactionEntityFactory transactionEntityFactory;
    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;
    @Mock
    private Appender<ILoggingEvent> mockAppender;

    private EventDigestHandler eventDigestHandler;
    private EventDigest eventDigest;

    @BeforeEach
    public void setUp() {
        transactionEntityFactory = new TransactionEntityFactory(new ObjectMapper());
        eventDigestHandler =  new EventDigestHandler(eventService, transactionService,
                transactionMetadataService, payoutService, transactionEntityFactory);
        eventDigest = EventDigest.fromEventList(List.of(anEventFixture().toEntity()));
        lenient().when(eventService.getEventDigestForResource(any(Event.class)))
                .thenReturn(eventDigest);
    }

    @Test
    public void shouldUpsertTransactionIfResourceTypeIsPayment() {
        Event event = anEventFixture().withResourceType(PAYMENT).toEntity();
        eventDigestHandler.processEvent(event);

        verify(eventService).getEventDigestForResource(event);
        verify(transactionService).upsertTransactionFor(eventDigest);
        verify(transactionMetadataService).upsertMetadataFor(event);
    }

    @Test
    public void shouldUpsertTransactionIfResourceTypeIsRefund() {
        Event event = anEventFixture().withResourceType(REFUND).toEntity();
        eventDigestHandler.processEvent(event);

        verify(eventService).getEventDigestForResource(event);
        verify(transactionService).upsertTransactionFor(any());
    }

    @Test
    public void shouldUpsertPayoutIfResourceTypeIsPayout() {
        Event event = anEventFixture().withResourceType(PAYOUT).toEntity();
        eventDigestHandler.processEvent(event);

        verify(eventService).getEventDigestForResource(event);
        verify(payoutService).upsertPayoutFor(eventDigest);
    }

    @Test
    public void shouldLogErrorAndThrowExceptionIfResourceTypeIsNotSupported() {
        Logger root = (Logger) LoggerFactory.getLogger(EventDigestHandler.class);
        root.addAppender(mockAppender);

        Event event = anEventFixture().withResourceType(AGREEMENT).toEntity();

        assertThrows(RuntimeException.class, () -> eventDigestHandler.processEvent(event));

        verify(transactionService, never()).upsertTransactionFor(any());
        verify(transactionMetadataService, never()).upsertMetadataFor(any());
        verify(payoutService, never()).upsertPayoutFor(any());

        verify(mockAppender, times(1)).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(loggingEvents.get(0).getFormattedMessage(),
                is(format("Event digest processing for resource type [%s] is not supported. " +
                                "Event type [%s] and resource external id [%s]",
                        event.getResourceType(), event.getEventType(), event.getResourceExternalId())));
    }
}