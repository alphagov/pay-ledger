package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.ledger.transaction.search.model.SettlementSummary;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Dispute extends Transaction {
    private final TransactionState state;
    private final ZonedDateTime createdDate;
    private final Integer eventCount;
    private final Long netAmount;
    private final Long fee;
    private final String gatewayTransactionId;
    private final ZonedDateTime evidenceDueDate;
    private final String reason;
    private final SettlementSummary settlementSummary;
    private final Payment paymentDetails;
    private final String parentTransactionId;

    private Dispute(Builder builder) {
        super(builder.id, builder.gatewayAccountId, builder.serviceId, builder.live, builder.amount,
                builder.externalId, builder.gatewayPayoutId);
        this.state = builder.state;
        this.createdDate = builder.createdDate;
        this.eventCount = builder.eventCount;
        this.netAmount = builder.netAmount;
        this.fee = builder.fee;
        this.gatewayTransactionId = builder.gatewayTransactionId;
        this.evidenceDueDate = builder.evidenceDueDate;
        this.reason = builder.reason;
        this.settlementSummary = builder.settlementSummary;
        this.paymentDetails = builder.paymentDetails;
        this.parentTransactionId = builder.parentTransactionId;
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

    public Long getNetAmount() {
        return netAmount;
    }

    public Long getFee() {
        return fee;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public ZonedDateTime getEvidenceDueDate() {
        return evidenceDueDate;
    }

    public String getReason() {
        return reason;
    }

    public SettlementSummary getSettlementSummary() {
        return settlementSummary;
    }

    public Payment getPaymentDetails() {
        return paymentDetails;
    }

    public String getParentTransactionId() {
        return parentTransactionId;
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.DISPUTE;
    }

    public static class Builder {
        private Long id;
        private String gatewayAccountId;
        private String serviceId;
        private Boolean live;
        private Long amount;
        private String externalId;
        private String gatewayPayoutId;
        private TransactionState state;
        private ZonedDateTime createdDate;
        private Integer eventCount;
        private Long netAmount;
        private Long fee;
        private String gatewayTransactionId;
        private ZonedDateTime evidenceDueDate;
        private String reason;
        private SettlementSummary settlementSummary;
        private Payment paymentDetails;
        private String parentTransactionId;

        public Dispute build() {
            return new Dispute(this);
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withGatewayAccountId(String gatewayAccountId) {
            this.gatewayAccountId = gatewayAccountId;
            return this;
        }

        public Builder withServiceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder withLive(Boolean live) {
            this.live = live;
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

        public Builder withGatewayPayoutId(String gatewayPayoutId) {
            this.gatewayPayoutId = gatewayPayoutId;
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

        public Builder withNetAmount(Long netAmount) {
            this.netAmount = netAmount;
            return this;
        }

        public Builder withFee(Long fee) {
            this.fee = fee;
            return this;
        }

        public Builder withGatewayTransactionId(String gatewayTransactionId) {
            this.gatewayTransactionId = gatewayTransactionId;
            return this;
        }

        public Builder withEvidenceDueDate(ZonedDateTime evidenceDueDate) {
            this.evidenceDueDate = evidenceDueDate;
            return this;
        }

        public Builder withReason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder withSettlementSummary(SettlementSummary settlementSummary) {
            this.settlementSummary = settlementSummary;
            return this;
        }

        public Builder withPaymentDetails(Payment paymentDetails) {
            this.paymentDetails = paymentDetails;
            return this;
        }

        public Builder withParentTransactionId(String parentExternalId) {
            this.parentTransactionId = parentExternalId;
            return this;
        }
    }
}
