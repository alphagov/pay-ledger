package uk.gov.pay.ledger.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.queue.sqs.SqsQueueService;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EventQueue {

    private static final String EVENT_MESSAGE_ATTRIBUTE_NAME = "All";
    private static final Logger LOGGER = LoggerFactory.getLogger(EventQueue.class);

    private SqsQueueService sqsQueueService;
    private final String eventQueueUrl;
    private ObjectMapper objectMapper;
    private int retryDelayInSeconds;

    @Inject
    public EventQueue(SqsQueueService sqsQueueService, LedgerConfig configuration, ObjectMapper objectMapper) {
        this.sqsQueueService = sqsQueueService;
        this.eventQueueUrl = configuration.getSqsConfig().getEventQueueUrl();
        this.objectMapper = objectMapper;
        this.retryDelayInSeconds = configuration.getQueueMessageReceiverConfig().getMessageRetryDelayInSeconds();
    }

    public List<EventMessage> retrieveEvents() throws QueueException {
        List<QueueMessage> queueMessages = sqsQueueService
                .receiveMessages(this.eventQueueUrl, EVENT_MESSAGE_ATTRIBUTE_NAME);

        return queueMessages
                .stream()
                .map(this::getMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void markMessageAsProcessed(EventMessage message) throws QueueException {
        sqsQueueService.deleteMessage(this.eventQueueUrl, message.getQueueMessageReceiptHandle());
    }

    public void scheduleMessageForRetry(EventMessage message) throws QueueException {
        sqsQueueService.deferMessage(this.eventQueueUrl, message.getQueueMessageReceiptHandle(), retryDelayInSeconds);
    }

    private EventMessage getMessage(QueueMessage queueMessage) {
        try {
            var eventDto = objectMapper.readValue(queueMessage.getMessageBody(), EventMessageDto.class);

            return EventMessage.of(eventDto, queueMessage);
        } catch (IOException e) {
            LOGGER.warn(
                    "There was an exception parsing the payload [{}] into an [{}]",
                    queueMessage.getMessageBody(),
                    EventMessage.class);

            //todo: should we retry or remove from the queue?
            return null;
        }
    }
}
