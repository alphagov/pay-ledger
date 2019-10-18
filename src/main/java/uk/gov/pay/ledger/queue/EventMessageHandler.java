package uk.gov.pay.ledger.queue;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.response.CreateEventResponse;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import java.util.List;

public class EventMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventMessageHandler.class);
    private EventQueue eventQueue;
    private EventService eventService;
    private TransactionService transactionService;

    @Inject
    public EventMessageHandler(EventQueue eventQueue, EventService eventService, TransactionService transactionService) {
        this.eventQueue = eventQueue;
        this.eventService = eventService;
        this.transactionService = transactionService;
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

    void processSingleMessage(EventMessage message) throws QueueException {
        Event event = message.getEvent();
        CreateEventResponse response = eventService.createOrUpdateIfExists(event);

        if(response.isSuccessful()) {
            EventDigest eventDigest = eventService.getEventDigestForResource(event.getResourceExternalId());
            transactionService.upsertTransactionFor(eventDigest);
            eventQueue.markMessageAsProcessed(message);
            LOGGER.info("The event message has been processed. [id={}] [state={}]",
                    message.getId(),
                    response.getState());
        } else {
            eventQueue.scheduleMessageForRetry(message);
            LOGGER.warn("The event message has been scheduled for retry. [id={}] [state={}] [error={}]",
                    message.getId(),
                    response.getState(),
                    response.getErrorMessage());
        }
    }
}
