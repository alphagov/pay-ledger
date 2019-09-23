package uk.gov.pay.ledger.transaction.model;

import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;
import java.util.Optional;

public class Refund extends Transaction {
    private final String reference;
    private final String description;
    private final TransactionState state;
    private final ZonedDateTime createdDate;
    private final Integer eventCount;
    private final String refundedBy;
    private final String parentExternalId;
    private final Optional<Transaction> parentTransaction;

    public Refund(Builder builder) {
        super(builder.id, builder.gatewayAccountId, builder.amount, builder.externalId);
        this.reference = builder.reference;
        this.description = builder.description;
        this.state = builder.state;
        this.createdDate = builder.createdDate;
        this.eventCount = builder.eventCount;
        this.refundedBy = builder.refundedBy;
        this.parentExternalId = builder.parentExternalId;
        this.parentTransaction = builder.parentTransaction;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public TransactionState getState() {
        return state;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public String getRefundedBy() {
        return refundedBy;
    }

    public String getParentExternalId() {
        return parentExternalId;
    }

    public Optional<Transaction> getParentTransaction() {
        return parentTransaction;
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.REFUND;
    }

    public static class Builder {
        private String reference;
        private String description;
        private TransactionState state;
        private ZonedDateTime createdDate;
        private Integer eventCount;
        private String refundedBy;
        private Long id;
        private String gatewayAccountId;
        private Long amount;
        private String externalId;
        private String parentExternalId;
        private Optional<Transaction> parentTransaction;

        public Builder() {
        }

        public Refund build() {
            return new Refund(this);
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withGatewayAccountId(String gatewayAccountId) {
            this.gatewayAccountId = gatewayAccountId;
            return this;
        }

        public Builder withAmount(Long amount) {
            this.amount = amount;
            return this;
        }

        public Builder withExternalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder withReference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withState(TransactionState state) {
            this.state = state;
            return this;
        }

        public Builder withCreatedDate(ZonedDateTime createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public Builder withEventCount(Integer eventCount) {
            this.eventCount = eventCount;
            return this;
        }

        public Builder withRefundedBy(String refundedBy) {
            this.refundedBy = refundedBy;
            return this;
        }

        public Builder withParentExternalId(String parentExternalId) {
            this.parentExternalId = parentExternalId;
            return this;
        }

        public Builder withParentTransaction(Optional<Transaction> transaction) {
            this.parentTransaction = transaction;
            return this;
        }
    }
}
