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

    public Optional<String> getQueueMessageId() {
        return Optional.ofNullable(queueMessage).map(QueueMessage::getMessageId);
    }

    public Event getEvent() {
        return new Event(
                getQueueMessageId().orElse(null),
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

    public Optional<String> getQueueMessageReceiptHandle() {
        return Optional.ofNullable(queueMessage).map(QueueMessage::getReceiptHandle);
    }

    public EventMessageDto getEventDto() {
        return eventDto;
    }
}