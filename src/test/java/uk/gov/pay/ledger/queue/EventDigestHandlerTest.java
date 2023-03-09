package uk.gov.pay.ledger.queue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.dao.entity.EventEntity;
import uk.gov.pay.ledger.queue.eventprocessor.AgreementEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.ChildTransactionEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.PaymentEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.PaymentInstrumentEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.PayoutEventProcessor;

import java.util.List;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.pay.ledger.event.model.ResourceType.AGREEMENT;
import static uk.gov.pay.ledger.event.model.ResourceType.DISPUTE;
import static uk.gov.pay.ledger.event.model.ResourceType.PAYMENT;
import static uk.gov.pay.ledger.event.model.ResourceType.PAYMENT_INSTRUMENT;
import static uk.gov.pay.ledger.event.model.ResourceType.PAYOUT;
import static uk.gov.pay.ledger.event.model.ResourceType.REFUND;
import static uk.gov.pay.ledger.event.model.ResourceType.SERVICE;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;

@ExtendWith(MockitoExtension.class)
class EventDigestHandlerTest {

    @Mock
    private PaymentEventProcessor mockPaymentEventProcessor;
    @Mock
    private PayoutEventProcessor mockPayoutEventProcessor;
    @Mock
    private ChildTransactionEventProcessor mockChildTransactionEventProcessor;
    @Mock
    private AgreementEventProcessor mockAgreementEventProcessor;
    @Mock
    private PaymentInstrumentEventProcessor mockPaymentInstrumentEventProcessor;
    @Mock
    private Appender<ILoggingEvent> mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;

    @InjectMocks
    private EventDigestHandler eventDigestHandler;

    @Test
    void shouldProcessPaymentEvent() {
        EventEntity event = anEventFixture().withResourceType(PAYMENT).toEntity();
        eventDigestHandler.processEvent(event, true);
        verify(mockPaymentEventProcessor).process(event, true);
    }

    @Test
    void shouldProcessRefundEvent() {
        EventEntity event = anEventFixture().withResourceType(REFUND).toEntity();
        eventDigestHandler.processEvent(event, true);
        verify(mockChildTransactionEventProcessor).process(event, true);
    }

    @Test
    void shouldProcessDisputeEvent() {
        EventEntity event = anEventFixture().withResourceType(DISPUTE).toEntity();
        eventDigestHandler.processEvent(event, true);
        verify(mockChildTransactionEventProcessor).process(event, true);
    }

    @Test
    void shouldProcessPayoutEvent() {
        EventEntity event = anEventFixture().withResourceType(PAYOUT).toEntity();
        eventDigestHandler.processEvent(event, true);
        verify(mockPayoutEventProcessor).process(event, true);
    }

    @Test
    void shouldProcessAgreementEvent() {
        EventEntity event = anEventFixture().withResourceType(AGREEMENT).toEntity();
        eventDigestHandler.processEvent(event, true);
        verify(mockAgreementEventProcessor).process(event, true);
    }

    @Test
    void shouldProcessPaymentInstrumentEvent() {
        EventEntity event = anEventFixture().withResourceType(PAYMENT_INSTRUMENT).toEntity();
        eventDigestHandler.processEvent(event, true);
        verify(mockPaymentInstrumentEventProcessor).process(event, true);
    }

    @Test
    void shouldLogErrorAndThrowExceptionIfResourceTypeIsNotSupported() {
        Logger root = (Logger) LoggerFactory.getLogger(EventDigestHandler.class);
        root.addAppender(mockAppender);

        EventEntity event = anEventFixture().withResourceType(SERVICE).toEntity();

        assertThrows(RuntimeException.class, () -> eventDigestHandler.processEvent(event, true));

        verify(mockAppender, times(1)).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(loggingEvents.get(0).getFormattedMessage(),
                is(format("Event digest processing for resource type [%s] is not supported. " +
                                "Event type [%s] and resource external id [%s]",
                        event.getResourceType(), event.getEventType(), event.getResourceExternalId())));
    }
}
