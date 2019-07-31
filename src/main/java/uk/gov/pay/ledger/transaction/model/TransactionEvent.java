package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.commons.api.json.ApiResponseDateTimeSerializer;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TransactionEvent {

    @JsonIgnore
    private final String externalId;
    private final Long amount;
    private final TransactionState state;
    private final ResourceType resourceType;
    private final String eventType;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private final ZonedDateTime timestamp;
    private final String data;

    private TransactionEvent(TransactionEventBuilder builder) {
        this.externalId = builder.externalId;
        this.amount = builder.amount;
        this.resourceType = builder.resourceType;
        this.state = builder.state;
        this.eventType = builder.eventType;
        this.timestamp = builder.timestamp;
        this.data = builder.data;
    }

    public static TransactionEvent from(TransactionEntity transactionEntity, Event event) {
        return TransactionEventBuilder.aTransactionEvent()
                .withExternalId(transactionEntity.getExternalId())
                .withAmount(transactionEntity.getAmount())
                .withState(SalientEventType.from(event.getEventType()).map(TransactionState::fromEventType).orElse(null))
                .withResourceType(event.getResourceType())
                .withEventType(event.getEventType())
                .withTimestamp(event.getEventDate())
                .withData(event.getEventData()).build();
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

    public String getResourceType() {
        return resourceType.toString().toUpperCase();
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

    public static final class TransactionEventBuilder {
        private String externalId;
        private Long amount;
        private TransactionState state;
        private ResourceType resourceType;
        private String eventType;
        private ZonedDateTime timestamp;
        private String data;

        private TransactionEventBuilder() {
        }

        static TransactionEventBuilder aTransactionEvent() {
            return new TransactionEventBuilder();
        }

        public TransactionEventBuilder withExternalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public TransactionEventBuilder withAmount(Long amount) {
            this.amount = amount;
            return this;
        }

        public TransactionEventBuilder withState(TransactionState state) {
            this.state = state;
            return this;
        }

        public TransactionEventBuilder withResourceType(ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public TransactionEventBuilder withEventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        TransactionEventBuilder withTimestamp(ZonedDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        TransactionEventBuilder withData(String data) {
            this.data = data;
            return this;
        }

        public TransactionEvent build() {
            return new TransactionEvent(this);
        }
    }
}
