package uk.gov.pay.ledger.queue;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.response.CreateEventResponse;
import uk.gov.pay.ledger.event.service.EventService;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.pay.ledger.event.model.response.CreateEventResponse.ignoredEventResponse;

public class EventMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventMessageHandler.class);

    private final EventQueue eventQueue;
    private final EventService eventService;
    private final EventDigestHandler eventDigestHandler;
    private final MetricRegistry metricRegistry;

    @Inject
    public EventMessageHandler(EventQueue eventQueue,
                               EventService eventService,
                               EventDigestHandler eventDigestHandler,
                               MetricRegistry metricRegistry) {
        this.eventQueue = eventQueue;
        this.eventService = eventService;
        this.eventDigestHandler = eventDigestHandler;
        this.metricRegistry = metricRegistry;
    }

    public void handle() throws QueueException {
        List<EventMessage> eventMessages = eventQueue.retrieveEvents();

        for (EventMessage message : eventMessages) {
            try {
                processSingleMessage(message);
            } catch (Exception e) {
                Sentry.capture(e);
                LOGGER.warn("Error during handling the event message. [id={}] [queueMessageId={}] [errorMessage={}]",
                        message.getId(),
                        message.getQueueMessageId(),
                        e.getMessage()
                );
            }
        }
    }

    private void processSingleMessage(EventMessage message) throws QueueException {
        Event event = message.getEvent();

        CreateEventResponse response;

        // We don't persist events created by internal admins for re-projecting domain objects so as to not pollute
        // the event feed. This also means that any event data on the event will be ignored when processing, and only
        // previous events will be used when re-projecting the domain object.
        if (event.isReprojectDomainObject()) {
            response = ignoredEventResponse();
        } else {
            response = eventService.createIfDoesNotExist(event);
        }

        final long ingestLag = event.getEventDate().until(ZonedDateTime.now(), ChronoUnit.MICROS);

        if (response.isSuccessful()) {
            eventDigestHandler.processEvent(event);
            eventQueue.markMessageAsProcessed(message);
            metricRegistry.histogram("event-message-handler.ingest-lag-microseconds").update(ingestLag);
            LOGGER.info("The event message has been processed.",
                    kv("id", message.getId()),
                    kv("resource_external_id", event.getResourceExternalId()),
                    kv("state", response.getState()),
                    kv("ingest_lag_micro_seconds", ingestLag),
                    kv("reproject_domain_object_event", event.isReprojectDomainObject()));
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
