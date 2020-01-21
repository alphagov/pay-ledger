package uk.gov.pay.ledger.queue;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.response.CreateEventResponse;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.transaction.service.TransactionService;
import uk.gov.pay.ledger.transactionmetadata.service.TransactionMetadataService;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;

public class EventMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventMessageHandler.class);

    private final EventQueue eventQueue;
    private final EventService eventService;
    private final TransactionService transactionService;
    private final MetricRegistry metricRegistry;
    private ObjectMapper objectMapper;
    private TransactionMetadataService transactionMetadataService;

    @Inject
    public EventMessageHandler(EventQueue eventQueue,
                               EventService eventService,
                               TransactionService transactionService,
                               MetricRegistry metricRegistry,
                               ObjectMapper objectMapper,
                               TransactionMetadataService transactionMetadataService) {
        this.eventQueue = eventQueue;
        this.eventService = eventService;
        this.transactionService = transactionService;
        this.metricRegistry = metricRegistry;
        this.objectMapper = objectMapper;
        this.transactionMetadataService = transactionMetadataService;
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
        Event event = message.getEvent();
        CreateEventResponse response = eventService.createIfDoesNotExist(event);

        final long ingestLag = event.getEventDate().until(ZonedDateTime.now(), ChronoUnit.MICROS);

        if(response.isSuccessful()) {
            EventDigest eventDigest = eventService.getEventDigestForResource(event.getResourceExternalId());

            transactionService.upsertTransactionFor(eventDigest);

            // process transaction metadata
            JsonNode eventDataNode = new ObjectMapper().readTree(event.getEventData());
            if (eventDataNode!=null && eventDataNode.has("external_metadata")) {
                transactionMetadataService.processTransactionMetadata(event.getResourceExternalId(), eventDataNode.get("external_metadata"));
            }
            eventQueue.markMessageAsProcessed(message);
            metricRegistry.histogram("event-message-handler.ingest-lag-microseconds").update(ingestLag);
            LOGGER.info("The event message has been processed.",
                    kv("id", message.getId()),
                    kv("resource_external_id", message.getEvent().getResourceExternalId()),
                    kv("state", response.getState()),
                    kv("ingest_lag_micro_seconds", ingestLag));
        } else {
            eventQueue.scheduleMessageForRetry(message);
            LOGGER.warn("The event message has been scheduled for retry.",
                    kv("id", message.getId()),
                    kv("resource_external_id", message.getEvent().getResourceExternalId()),
                    kv("state", response.getState()),
                    kv("error", response.getErrorMessage()));
        }
    }
}
