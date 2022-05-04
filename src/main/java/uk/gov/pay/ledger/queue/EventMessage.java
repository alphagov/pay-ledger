package uk.gov.pay.ledger.queue;

import uk.gov.pay.ledger.event.model.Event;
import uk.gov.service.payments.commons.queue.model.QueueMessage;

import java.util.Optional;

public class EventMessage {
    private EventMessageDto eventDto;
    private QueueMessage queueMessage;

    public EventMessage(EventMessageDto eventDto, QueueMessage queueMessage) {
        this.eventDto = eventDto;
        this.queueMessage = queueMessage;
    }

    public static EventMessage of(EventMessageDto eventDto, QueueMessage queueMessage) {
        return new EventMessage(eventDto, queueMessage);
    }

    public String getQueueMessageId() {
        return Optional.ofNullable(queueMessage)
                .map(QueueMessage::getMessageId)
                .orElse(null);
    }

    public Event getEvent() {
        return new Event(
                getQueueMessageId(),
                eventDto.getServiceId(),
                eventDto.isLive(),
                eventDto.getResourceType(),
                eventDto.getExternalId(),
                eventDto.getParentExternalId(),
                eventDto.getTimestamp(),
                eventDto.getEventType(),
                eventDto.getEventData(),
                eventDto.isReprojectDomainObject()
        );
    }

    public String getQueueMessageReceiptHandle() {
        return Optional.of(queueMessage)
                .map(QueueMessage::getReceiptHandle)
                .orElse(null);
    }

    public EventMessageDto getEventDto() {
        return eventDto;
    }
}