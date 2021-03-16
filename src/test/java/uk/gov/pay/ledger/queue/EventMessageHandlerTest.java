package uk.gov.pay.ledger.queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.response.CreateEventResponse;
import uk.gov.pay.ledger.event.service.EventService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @InjectMocks
    private EventMessageHandler eventMessageHandler;

    @BeforeEach
    void setUp() throws QueueException {
        when(eventQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
    }

    @Test
    void shouldMarkMessageAsProcessed_WhenEventIsProcessedSuccessfully() throws QueueException {
        Event event = aQueuePaymentEventFixture().toEntity();
        when(eventMessage.getEvent()).thenReturn(event);
        when(eventService.createIfDoesNotExist(any())).thenReturn(createEventResponse);
        when(createEventResponse.isSuccessful()).thenReturn(true);

        eventMessageHandler.handle();

        verify(eventDigestHandler).processEvent(event);
        verify(eventQueue).markMessageAsProcessed(any(EventMessage.class));
    }

    @Test
    void shouldMarkMessageAsProcessedAndNotInsert_WhenReprojectDomainObjectEvent() throws QueueException {
        Event event = aQueuePaymentEventFixture().withIsReprojectDomainObject(true).toEntity();
        when(eventMessage.getEvent()).thenReturn(event);
        
        eventMessageHandler.handle();
        verify(eventDigestHandler).processEvent(event);
        verify(eventQueue).markMessageAsProcessed(any(EventMessage.class));
        verify(eventService, never()).createIfDoesNotExist(any());
    }

    @Test
    void shouldScheduleMessageForRetry_WhenEventIsNotProcessedSuccessfully() throws QueueException {
        Event event = aQueuePaymentEventFixture().toEntity();
        when(eventMessage.getEvent()).thenReturn(event);
        when(eventService.createIfDoesNotExist(any())).thenReturn(createEventResponse);
        when(createEventResponse.isSuccessful()).thenReturn(false);

        eventMessageHandler.handle();

        verify(eventQueue).scheduleMessageForRetry(any());
    }
}