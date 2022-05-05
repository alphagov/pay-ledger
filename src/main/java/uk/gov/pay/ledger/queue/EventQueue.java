package uk.gov.pay.ledger.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.service.payments.commons.queue.exception.QueueException;
import uk.gov.service.payments.commons.queue.model.QueueMessage;
import uk.gov.service.payments.commons.queue.sqs.SqsQueueService;

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
        sqsQueueService.deleteMessage(this.eventQueueUrl, message.getQueueMessageReceiptHandle().orElse(null));
    }

    public void scheduleMessageForRetry(EventMessage message) throws QueueException {
        sqsQueueService.deferMessage(this.eventQueueUrl, message.getQueueMessageReceiptHandle().orElse(null), retryDelayInSeconds);
    }

    private EventMessage getMessage(QueueMessage queueMessage) {
        try {
            EventMessageDto eventDto = objectMapper.readValue(queueMessage.getMessageBody(), EventMessageDto.class);

            return EventMessage.of(eventDto, queueMessage);
        } catch (IOException e) {
            LOGGER.warn(
                    "There was an exception parsing message [messageId={}] into an [{}]",
                    queueMessage.getMessageId(),
                    EventMessage.class);

            return null;
        }
    }
}