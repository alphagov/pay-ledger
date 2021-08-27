package uk.gov.pay.ledger.transaction.model;

import uk.gov.pay.ledger.transaction.search.model.SettlementSummary;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;

public class Refund extends Transaction {
    private final TransactionState state;
    private final ZonedDateTime createdDate;
    private final Integer eventCount;
    private final String refundedBy;
    private final String refundedByUserEmail;
    private final String parentExternalId;
    private final String gatewayTransactionId;
    private final Payment paymentDetails;
    private final SettlementSummary settlementSummary;

    public Refund(Builder builder) {
        super(builder.id, builder.gatewayAccountId, builder.amount, builder.externalId, builder.gatewayPayoutId);
        this.state = builder.state;
        this.createdDate = builder.createdDate;
        this.eventCount = builder.eventCount;
        this.refundedBy = builder.refundedBy;
        this.refundedByUserEmail = builder.refundedByUserEmail;
        this.parentExternalId = builder.parentExternalId;
        this.gatewayTransactionId = builder.gatewayTransactionId;
        this.paymentDetails = builder.paymentDetails;
        this.settlementSummary = builder.settlementSummary;
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

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.REFUND;
    }

    public String getRefundedByUserEmail() {
        return refundedByUserEmail;
    }

    public Payment getPaymentDetails() {
        return paymentDetails;
    }

    public SettlementSummary getSettlementSummary() {
        return settlementSummary;
    }

    public static class Builder {
        private TransactionState state;
        private ZonedDateTime createdDate;
        private Integer eventCount;
        private String refundedBy;
        private String refundedByUserEmail;
        private Long id;
        private String gatewayAccountId;
        private Long amount;
        private String externalId;
        private String parentExternalId;
        private Payment paymentDetails;
        private String gatewayPayoutId;
        private String gatewayTransactionId;
        private SettlementSummary settlementSummary;

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

        public Builder withRefundedByUserEmail(String refundedByUserEmail) {
            this.refundedByUserEmail = refundedByUserEmail;
            return this;
        }

        public Builder withParentExternalId(String parentExternalId) {
            this.parentExternalId = parentExternalId;
            return this;
        }

        public Builder withGatewayPayoutId(String gatewayPayoutId) {
            this.gatewayPayoutId = gatewayPayoutId;
            return this;
        }

        public Builder withGatewayTransactionId(String gatewayTransactionId) {
            this.gatewayTransactionId = gatewayTransactionId;
            return this;
        }

        public Builder withPaymentDetails(Payment paymentDetails) {
            this.paymentDetails = paymentDetails;
            return this;
        }

        public Builder withSettlementSummary(SettlementSummary refundSettlementSummary) {
            this.settlementSummary = refundSettlementSummary;
            return this;
        }
    }
}
