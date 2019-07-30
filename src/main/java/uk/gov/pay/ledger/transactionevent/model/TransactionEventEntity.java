package uk.gov.pay.ledger.transactionevent.model;

import java.time.ZonedDateTime;

public class TransactionEventEntity {

    private String externalId;
    private String transactionType;
    private Long amount;
    private ZonedDateTime eventDate;
    private String eventType;
    private String eventData;

    public TransactionEventEntity() {
    }

    public TransactionEventEntity(TransactionEventEntity.Builder builder) {
        this.externalId = builder.externalId;
        this.amount = builder.amount;
        this.transactionType = builder.transactionType;
        this.eventDate = builder.eventDate;
        this.eventData = builder.eventData;
        this.eventType = builder.eventType;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public Long getAmount() {
        return amount;
    }

    public ZonedDateTime getEventDate() {
        return eventDate;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventData() {
        return eventData;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public void setEventDate(ZonedDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public static class Builder {
        private String externalId;
        private String transactionType;
        private Long amount;
        private ZonedDateTime eventDate;
        private String eventType;
        private String eventData;

        public Builder() {
        }

        public TransactionEventEntity build() {
            return new TransactionEventEntity(this);
        }

        public TransactionEventEntity.Builder withExternalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public TransactionEventEntity.Builder withAmount(Long amount) {
            this.amount = amount;
            return this;
        }

        public TransactionEventEntity.Builder withTransactionType(String transactionType) {
            this.transactionType = transactionType;
            return this;
        }

        public TransactionEventEntity.Builder withEventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public TransactionEventEntity.Builder withEventData(String eventData) {
            this.eventData = eventData;
            return this;
        }

        public TransactionEventEntity.Builder withEventDate(ZonedDateTime eventDate) {
            this.eventDate = eventDate;
            return this;
        }
    }
}
