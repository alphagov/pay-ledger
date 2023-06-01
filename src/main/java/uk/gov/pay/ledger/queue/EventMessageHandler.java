package uk.gov.pay.ledger.queue;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import io.sentry.Sentry;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.event.entity.EventEntity;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.event.model.response.CreateEventResponse;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.eventpublisher.EventPublisher;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.pay.ledger.event.model.ResourceType.DISPUTE;
import static uk.gov.pay.ledger.event.model.ResourceType.PAYMENT;
import static uk.gov.pay.ledger.event.model.ResourceType.REFUND;
import static uk.gov.pay.ledger.event.model.response.CreateEventResponse.CreateEventState.INSERTED;
import static uk.gov.pay.ledger.event.model.response.CreateEventResponse.ignoredEventResponse;
import static uk.gov.pay.ledger.eventpublisher.TopicName.CARD_PAYMENT_DISPUTE_EVENTS;
import static uk.gov.pay.ledger.eventpublisher.TopicName.CARD_PAYMENT_EVENTS;
import static uk.gov.service.payments.logging.LoggingKeys.LEDGER_EVENT_TYPE;
import static uk.gov.service.payments.logging.LoggingKeys.RESOURCE_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.SQS_MESSAGE_ID;

public class EventMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventMessageHandler.class);

    private final EventQueue eventQueue;
    private final EventService eventService;
    private final EventDigestHandler eventDigestHandler;
    private final EventPublisher eventPublisher;
    private final MetricRegistry metricRegistry;
    private final LedgerConfig ledgerConfig;
    private final Jdbi jdbi;

    @Inject
    public EventMessageHandler(EventQueue eventQueue,
                               EventService eventService,
                               EventDigestHandler eventDigestHandler,
                               EventPublisher eventPublisher,
                               MetricRegistry metricRegistry,
                               LedgerConfig ledgerConfig,
                               Jdbi jdbi) {
        this.eventQueue = eventQueue;
        this.eventService = eventService;
        this.eventDigestHandler = eventDigestHandler;
        this.eventPublisher = eventPublisher;
        this.metricRegistry = metricRegistry;
        this.ledgerConfig = ledgerConfig;
        this.jdbi = jdbi;
    }

    public void handle() throws QueueException {
        List<EventMessage> eventMessages = eventQueue.retrieveEvents();

        for (EventMessage message : eventMessages) {
            try {
                processSingleMessage(message);
            } catch (Exception e) {
                Sentry.captureException(e);
                LOGGER.warn("Error during handling the event message",
                        kv(SQS_MESSAGE_ID, message.getQueueMessageId()),
                        kv(RESOURCE_EXTERNAL_ID, message.getEvent().getResourceExternalId()),
                        kv(LEDGER_EVENT_TYPE, message.getEvent().getEventType()),
                        kv("error", e.getMessage())
                );
            }
        }
    }

    // provides a transactional guarantee, if any of the events fail to process, none of the events will be persisted
    public void processEventBatch(List<EventMessage> messages) throws QueueException {
        jdbi.useTransaction(handle -> {
            for (EventMessage message : messages) {
                processSingleMessage(message);
            }
        });
    }

    private void processSingleMessage(EventMessage message) throws QueueException {
        EventEntity event = message.getEvent();

        CreateEventResponse response;

        // We don't persist events created by internal admins for re-projecting domain objects so as to not pollute
        // the event feed. This also means that any event data on the event will be ignored when processing, and only
        // previous events will be used when re-projecting the domain object.
        if (event.isReprojectDomainObject()) {
            response = ignoredEventResponse();
        } else {
            response = eventService.createIfDoesNotExist(event);
        }

        publishEventToSNS(message, event);

        final long ingestLag = event.getEventDate().until(ZonedDateTime.now(), ChronoUnit.MICROS);

        if (response.isSuccessful()) {
            eventDigestHandler.processEvent(event, response.getState() == INSERTED);
            if (message.getQueueMessageReceiptHandle().isPresent()) {
                eventQueue.markMessageAsProcessed(message);
            }
            metricRegistry.histogram("event-message-handler.ingest-lag-microseconds").update(ingestLag);
            var loggingArgs = new ArrayList<>(List.of(
                    kv(SQS_MESSAGE_ID, message.getQueueMessageId()),
                    kv(RESOURCE_EXTERNAL_ID, event.getResourceExternalId()),
                    kv(LEDGER_EVENT_TYPE, event.getEventType()),
                    kv("state", response.getState()),
                    kv("ingest_lag_micro_seconds", ingestLag)));

            if (event.isReprojectDomainObject()) {
                loggingArgs.add(kv("reproject_domain_object_event", event.isReprojectDomainObject()));
            }

            LOGGER.info("The event message has been processed.", loggingArgs.toArray());
        } else {
            if (message.getQueueMessageId().isPresent()) {
                eventQueue.scheduleMessageForRetry(message);
                LOGGER.warn("The event message has been scheduled for retry.",
                        kv(SQS_MESSAGE_ID, message.getQueueMessageId()),
                        kv(RESOURCE_EXTERNAL_ID, event.getResourceExternalId()),
                        kv(LEDGER_EVENT_TYPE, event.getEventType()),
                        kv("state", response.getState()),
                        kv("error", response.getErrorMessage()));
            } else {
                LOGGER.warn("Create event response was unsuccessful.",
                        kv(RESOURCE_EXTERNAL_ID, event.getResourceExternalId()),
                        kv("state", response.getState()),
                        kv("error", response.getErrorMessage()));
            }
        }
    }

    private void publishEventToSNS(EventMessage message, EventEntity event) {
        if (ledgerConfig.getSnsConfig().isSnsEnabled()) {
            try {
                ResourceType resourceType = message.getEvent().getResourceType();
                if (resourceType == DISPUTE) {
                    if (ledgerConfig.getSnsConfig().isPublishCardPaymentDisputeEventsToSns()) {
                        eventPublisher.publishMessageToTopic(message.getRawMessageBody(), CARD_PAYMENT_DISPUTE_EVENTS);
                        LOGGER.info("Published message to SNS topic",
                                kv("sns_topic", CARD_PAYMENT_DISPUTE_EVENTS),
                                kv(SQS_MESSAGE_ID, message.getQueueMessageId()),
                                kv(RESOURCE_EXTERNAL_ID, event.getResourceExternalId()),
                                kv(LEDGER_EVENT_TYPE, event.getEventType()));
                    }
                } else {
                    if ((resourceType == PAYMENT || resourceType == REFUND)
                            && ledgerConfig.getSnsConfig().isPublishCardPaymentEventsToSns()) {

                        eventPublisher.publishMessageToTopic(message.getRawMessageBody(), CARD_PAYMENT_EVENTS);
                        LOGGER.info("Published message to SNS topic",
                                kv("sns_topic", CARD_PAYMENT_EVENTS),
                                kv(SQS_MESSAGE_ID, message.getQueueMessageId()),
                                kv(RESOURCE_EXTERNAL_ID, event.getResourceExternalId()),
                                kv(LEDGER_EVENT_TYPE, event.getEventType()));
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to publish event for message",
                        kv(SQS_MESSAGE_ID, message.getQueueMessageId()),
                        kv(RESOURCE_EXTERNAL_ID, event.getResourceExternalId()),
                        kv(LEDGER_EVENT_TYPE, event.getEventType()),
                        kv("error", e.getMessage()));
            }
        }
    }
}
