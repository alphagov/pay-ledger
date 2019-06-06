package uk.gov.pay.ledger.queue;

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

    public String getId() {
        return eventDto.getId();
    }

    public String getQueueMessageId() {
        return queueMessage.getMessageId();
    }

    //todo: not sure about it
    public EventMessageDto getEvent() {
        return eventDto;
    }

    public String getQueueMessageReceiptHandle() {
        return queueMessage.getReceiptHandle();
    }
}
