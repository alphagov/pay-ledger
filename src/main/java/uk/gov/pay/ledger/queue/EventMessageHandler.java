package uk.gov.pay.ledger.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.services.EventService;

import java.util.List;

public class EventMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventMessageHandler.class);
    private EventQueue eventQueue;
    private EventService eventService;

    @Inject
    public EventMessageHandler(EventQueue eventQueue, EventService eventService) {
        this.eventQueue = eventQueue;
        this.eventService = eventService;
    }

    public void handle() throws QueueException {
        List<EventMessage> eventMessages = eventQueue.retrieveEvents();

        for (EventMessage message : eventMessages) {
            try {
                processSingleMessage(message);
            } catch (Exception e) {
                LOGGER.warn("Error during handling the event message. [id={}] [queueMessageId={}] [errorMessage={}]",
                        message.getId(),
                        message.getQueueMessageId(),
                        e.getMessage()
                );
            }
        }
    }

    private void processSingleMessage(EventMessage message) throws QueueException, JsonProcessingException {
        var response = eventService.createIfDoesNotExist(message.getEvent());

        if(response.isSuccessful()) {
            eventQueue.markMessageAsProcessed(message);
            LOGGER.info("The event message has been processed. [id={}] [state={}]",
                    message.getId(),
                    response.getState());
        } else {
            eventQueue.scheduleMessageForRetry(message);
            LOGGER.info("The event message has been scheduled for retry. [id={}] [state={}] [error={}]",
                    message.getId(),
                    response.getState(),
                    response.getErrorMessage());
        }
    }
}
