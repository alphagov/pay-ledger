package uk.gov.pay.ledger.transaction.model;

import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;

public class TransactionEvent {

    private final String externalId;
    private final Long amount;
    private final TransactionState state;
    private final ResourceType resourceType;
    private final String eventType;
    private final ZonedDateTime timestamp;
    private final String data;

    public TransactionEvent(String externalId, Long amount, TransactionState state, ResourceType resourceType, String eventType, ZonedDateTime timestamp, String data) {
        this.externalId = externalId;
        this.amount = amount;
        this.resourceType = resourceType;
        this.state = state;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.data = data;
    }

    public static TransactionEvent from(TransactionEntity transactionEntity, Event event) {
        return new TransactionEvent(transactionEntity.getExternalId(),
                transactionEntity.getAmount(),
                SalientEventType.from(event.getEventType()).map(TransactionState::fromEventType).orElse(null),
                event.getResourceType(),
                event.getEventType(),
                event.getEventDate(),
                event.getEventData());
    }

    public String getExternalId() {
        return externalId;
    }

    public Long getAmount() {
        return amount;
    }

    public TransactionState getState() {
        return state;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getEventType() {
        return eventType;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public String getData() {
        return data;
    }
}
