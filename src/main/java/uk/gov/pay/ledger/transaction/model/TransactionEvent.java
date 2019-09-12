package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.commons.api.json.ApiResponseDateTimeSerializer;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.ExternalTransactionState;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TransactionEvent {

    private static Logger logger = LoggerFactory.getLogger(TransactionFactory.class);

    @JsonIgnore
    private final String externalId;
    private final Long amount;
    private final ExternalTransactionState state;
    private final ResourceType resourceType;
    private final String eventType;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private final ZonedDateTime timestamp;
    private final Map<String, Object> data;

    private TransactionEvent(TransactionEventBuilder builder) {
        this.externalId = builder.externalId;
        this.amount = builder.amount;
        this.resourceType = builder.resourceType;
        this.state = builder.state;
        this.eventType = builder.eventType;
        this.timestamp = builder.timestamp;
        this.data = builder.data;
    }

    public static TransactionEvent from(TransactionEntity transactionEntity, Event event, ObjectMapper objectMapper, int statusVersion) {
        try {
            ExternalTransactionState state = SalientEventType.from(event.getEventType())
                    .map(TransactionState::fromEventType)
                    .map(s -> ExternalTransactionState.from(s, statusVersion))
                    .orElse(null);

            return TransactionEventBuilder.aTransactionEvent()
                    .withExternalId(transactionEntity.getExternalId())
                    .withAmount(transactionEntity.getAmount())
                    .withState(state)
                    .withResourceType(event.getResourceType())
                    .withEventType(event.getEventType())
                    .withTimestamp(event.getEventDate())
                    .withData(objectMapper.readValue(event.getEventData(), new TypeReference<Map<String, Object>>() {
                    })).build();
        } catch (IOException e) {
            logger.error("Error parsing transaction event data [Transaction external ID - {}] [errorMessage={}]",
                    transactionEntity.getExternalId(),
                    e.getMessage());
        }
        return null;
    }

    public String getExternalId() {
        return externalId;
    }

    public Long getAmount() {
        return amount;
    }

    public ExternalTransactionState getState() {
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

    public Map<String, Object> getData() {
        return data;
    }

    public static final class TransactionEventBuilder {
        private String externalId;
        private Long amount;
        private ExternalTransactionState state;
        private ResourceType resourceType;
        private String eventType;
        private ZonedDateTime timestamp;
        private Map<String, Object> data;

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

        public TransactionEventBuilder withState(ExternalTransactionState state) {
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

        TransactionEventBuilder withData(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public TransactionEvent build() {
            return new TransactionEvent(this);
        }
    }
}
