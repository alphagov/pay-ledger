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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

@ExtendWith(MockitoExtension.class)
public class EventMessageHandlerTest {

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

    private Event event = aQueuePaymentEventFixture().toEntity();

    @InjectMocks
    private EventMessageHandler eventMessageHandler;

    @BeforeEach
    public void setUp() throws QueueException {
        when(eventQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
        when(eventService.createIfDoesNotExist(any())).thenReturn(createEventResponse);
        when(eventMessage.getEvent()).thenReturn(event);
    }

    @Test
    public void shouldMarkMessageAsProcessed_WhenEventIsProcessedSuccessfully() throws QueueException {
        when(createEventResponse.isSuccessful()).thenReturn(true);

        eventMessageHandler.handle();

        verify(eventQueue).markMessageAsProcessed(any(EventMessage.class));
    }

    @Test
    public void shouldScheduleMessageForRetry_WhenEventIsNotProcessedSuccessfully() throws QueueException {
        when(createEventResponse.isSuccessful()).thenReturn(false);

        eventMessageHandler.handle();

        verify(eventQueue).scheduleMessageForRetry(any());
    }
}