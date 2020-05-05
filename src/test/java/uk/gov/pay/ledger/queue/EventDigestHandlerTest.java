package uk.gov.pay.ledger.queue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.payout.service.PayoutService;
import uk.gov.pay.ledger.transaction.service.TransactionMetadataService;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import java.util.List;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.event.model.ResourceType.AGREEMENT;
import static uk.gov.pay.ledger.event.model.ResourceType.PAYMENT;
import static uk.gov.pay.ledger.event.model.ResourceType.PAYOUT;
import static uk.gov.pay.ledger.event.model.ResourceType.REFUND;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;

@RunWith(MockitoJUnitRunner.class)
public class EventDigestHandlerTest {

    @Mock
    private EventService eventService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private TransactionMetadataService transactionMetadataService;
    @Mock
    private PayoutService payoutService;
    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;
    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @InjectMocks
    private EventDigestHandler eventDigestHandler;
    private EventDigest eventDigest;

    @Before
    public void setUp() {
        eventDigest = EventDigest.fromEventList(List.of(anEventFixture().toEntity()));
        when(eventService.getEventDigestForResource(any()))
                .thenReturn(eventDigest);
    }

    @Test
    public void shouldUpsertTransactionIfResourceTypeIsPayment() {
        Event event = anEventFixture().withResourceType(PAYMENT).toEntity();
        eventDigestHandler.processEvent(event);

        verify(eventService).getEventDigestForResource(event.getResourceExternalId());
        verify(transactionService).upsertTransactionFor(eventDigest);
        verify(transactionMetadataService).upsertMetadataFor(event);
    }

    @Test
    public void shouldUpsertTransactionIfResourceTypeIsRefund() {
        Event event = anEventFixture().withResourceType(REFUND).toEntity();
        eventDigestHandler.processEvent(event);

        verify(eventService).getEventDigestForResource(event.getResourceExternalId());
        verify(transactionService).upsertTransactionFor(eventDigest);
        verify(transactionMetadataService).upsertMetadataFor(event);
    }

    @Test
    public void shouldUpsertPayoutIfResourceTypeIsPayout() {
        Event event = anEventFixture().withResourceType(PAYOUT).toEntity();
        eventDigestHandler.processEvent(event);

        verify(eventService).getEventDigestForResource(event.getResourceExternalId());
        verify(payoutService).upsertPayoutFor(eventDigest);
    }

    @Test
    public void shouldLogWarningIfResourceTypeIsNotPayout() {
        Logger root = (Logger) LoggerFactory.getLogger(EventDigestHandler.class);
        root.addAppender(mockAppender);

        Event event = anEventFixture().withResourceType(AGREEMENT).toEntity();
        eventDigestHandler.processEvent(event);

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