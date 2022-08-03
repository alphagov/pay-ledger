package uk.gov.pay.ledger.queue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.SnsConfig;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.event.model.response.CreateEventResponse;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.eventpublisher.EventPublisher;
import uk.gov.pay.ledger.eventpublisher.TopicName;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import java.util.List;
import java.util.Optional;

import static ch.qos.logback.classic.Level.INFO;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

@ExtendWith(MockitoExtension.class)
class EventMessageHandlerTest {

    @Mock
    private EventQueue eventQueue;

    @Mock
    private EventService eventService;

    @Mock
    private EventDigestHandler eventDigestHandler;

    @Mock
    private CreateEventResponse createEventResponse;

    @Mock
    private EventMessage eventMessage;

    @Mock
    private MetricRegistry metricRegistry;

    @Mock
    private Histogram histogram;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SnsConfig snsConfig;

    @Mock
    private LedgerConfig ledgerConfig;

    @Mock
    private EventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;

    @InjectMocks
    private EventMessageHandler eventMessageHandler;

    @BeforeEach
    void setUp() throws QueueException {
        when(eventQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
        when(ledgerConfig.getSnsConfig()).thenReturn(snsConfig);
        when(snsConfig.isSnsEnabled()).thenReturn(false);
    }

    @Test
    void shouldMarkMessageAsProcessed_WhenEventIsProcessedSuccessfully() throws QueueException {
        Event event = aQueuePaymentEventFixture().toEntity();
        when(eventMessage.getEvent()).thenReturn(event);
        when(eventMessage.getQueueMessageReceiptHandle()).thenReturn(Optional.of("a-valid-recipient-handle"));
        when(eventService.createIfDoesNotExist(any())).thenReturn(createEventResponse);
        when(createEventResponse.isSuccessful()).thenReturn(true);
        when(metricRegistry.histogram((any()))).thenReturn(histogram);

        eventMessageHandler.handle();

        verify(eventDigestHandler).processEvent(event, false);
        verify(eventQueue).markMessageAsProcessed(any(EventMessage.class));
    }

    @Test
    void shouldMarkMessageAsProcessedAndNotInsert_WhenReprojectDomainObjectEvent() throws QueueException {
        Logger root = (Logger) LoggerFactory.getLogger(EventMessageHandler.class);
        root.setLevel(INFO);
        root.addAppender(mockAppender);

        String eventType = "AN_EVENT_TYPE";
        Event event = aQueuePaymentEventFixture()
                .withIsReprojectDomainObject(true)
                .withEventType(eventType)
                .toEntity();
        when(eventMessage.getEvent()).thenReturn(event);
        when(eventMessage.getQueueMessageReceiptHandle()).thenReturn(Optional.of("a-valid-recipient-handle"));
        when(metricRegistry.histogram((any()))).thenReturn(histogram);

        eventMessageHandler.handle();
        verify(eventDigestHandler).processEvent(event, false);
        verify(eventQueue).markMessageAsProcessed(any(EventMessage.class));
        verify(eventService, never()).createIfDoesNotExist(any());

        verify(mockAppender, times(1)).doAppend(loggingEventArgumentCaptor.capture());
        assertThat(loggingEventArgumentCaptor.getValue().getArgumentArray(), hasItemInArray(kv("reproject_domain_object_event", true)));
        assertThat(loggingEventArgumentCaptor.getValue().getArgumentArray(), hasItemInArray(kv("event_type", eventType)));
    }

    @Test
    void shouldScheduleMessageForRetry_WhenEventIsNotProcessedSuccessfully() throws QueueException {
        Event event = aQueuePaymentEventFixture().toEntity();
        when(eventMessage.getEvent()).thenReturn(event);
        when(eventMessage.getQueueMessageId()).thenReturn(Optional.of("a-valid-queue-message-id"));
        when(eventService.createIfDoesNotExist(any())).thenReturn(createEventResponse);
        when(createEventResponse.isSuccessful()).thenReturn(false);

        eventMessageHandler.handle();

        verify(eventQueue).scheduleMessageForRetry(any());
    }

    @Test
    void shouldNotScheduleMessageForRetryGivenNoSQSQueueMessage_WhenEventIsNotProcessedSuccessfully() throws QueueException {
        Event event = aQueuePaymentEventFixture().toEntity();
        when(eventMessage.getEvent()).thenReturn(event);
        when(eventMessage.getQueueMessageId()).thenReturn(null);
        when(eventService.createIfDoesNotExist(any())).thenReturn(createEventResponse);
        when(createEventResponse.isSuccessful()).thenReturn(false);

        eventMessageHandler.handle();

        verifyNoMoreInteractions(eventQueue);
    }

    @Test
    void shouldPublishCardPaymentMessageWhenSnsEnabled() throws Exception {
        String messageBody = "{ \"foo\": \"bar\"}";

        Event event = aQueuePaymentEventFixture().toEntity();
        when(eventMessage.getEvent()).thenReturn(event);
        when(eventMessage.getRawMessageBody()).thenReturn(messageBody);
        when(ledgerConfig.getSnsConfig()).thenReturn(snsConfig);
        when(snsConfig.isSnsEnabled()).thenReturn(true);
        when(snsConfig.isPublishCardPaymentEventsToSns()).thenReturn(true);

        eventMessageHandler.handle();

        verify(eventPublisher).publishMessageToTopic(messageBody, TopicName.CARD_PAYMENT_EVENTS);
    }

    @Test
    void shouldPublishDisputeMessageWhenSnsEnabled() throws Exception {;
        String messageBody = "{ \"foo\": \"bar\"}";

        Event event = aQueuePaymentEventFixture().withResourceType(ResourceType.DISPUTE).toEntity();
        when(eventMessage.getEvent()).thenReturn(event);
        when(eventMessage.getRawMessageBody()).thenReturn(messageBody);
        when(ledgerConfig.getSnsConfig()).thenReturn(snsConfig);
        when(snsConfig.isSnsEnabled()).thenReturn(true);
        when(snsConfig.isPublishCardPaymentDisputeEventsToSns()).thenReturn(true);

        eventMessageHandler.handle();

        verify(eventPublisher).publishMessageToTopic(messageBody, TopicName.CARD_PAYMENT_DISPUTE_EVENTS);
    }

    @Test
    void shouldNotTryToPublishMessagesWhenSnsNotEnabled() throws QueueException {
        Event event = aQueuePaymentEventFixture().toEntity();
        when(eventMessage.getEvent()).thenReturn(event);
        when(ledgerConfig.getSnsConfig()).thenReturn(snsConfig);
        when(snsConfig.isSnsEnabled()).thenReturn(false);

        eventMessageHandler.handle();

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldNotTryToPublishWhenPublishCardPaymentEventsToSnsDisabled() throws QueueException {
        Event event = aQueuePaymentEventFixture().toEntity();
        when(eventMessage.getEvent()).thenReturn(event);
        when(ledgerConfig.getSnsConfig()).thenReturn(snsConfig);
        when(snsConfig.isSnsEnabled()).thenReturn(true);
        when(snsConfig.isPublishCardPaymentEventsToSns()).thenReturn(false);

        eventMessageHandler.handle();

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldNotTryToPublishWhenPublishCardPaymentDisputeEventsToSnsDisabled() throws QueueException {
        Event event = aQueuePaymentEventFixture().withResourceType(ResourceType.DISPUTE).toEntity();
        when(eventMessage.getEvent()).thenReturn(event);
        when(ledgerConfig.getSnsConfig()).thenReturn(snsConfig);
        when(snsConfig.isSnsEnabled()).thenReturn(true);
        when(snsConfig.isPublishCardPaymentDisputeEventsToSns()).thenReturn(false);

        eventMessageHandler.handle();

        verifyNoInteractions(eventPublisher);
    }
}