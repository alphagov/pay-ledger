package uk.gov.pay.ledger.payout.entity;

import java.time.ZonedDateTime;

public class PayoutEntity {

    private final Long id;
    private final String gatewayPayoutId;
    private final Long amount;
    private final ZonedDateTime createdDate;
    private final ZonedDateTime paidOutDate;
    private final String statementDescriptor;
    private final String status;
    private final String type;
    private final Long version;

    public PayoutEntity(PayoutEntityBuilder builder) {
        this.id = builder.id;
        this.gatewayPayoutId = builder.gatewayPayoutId;
        this.amount = builder.amount;
        this.createdDate = builder.createdDate;
        this.paidOutDate = builder.paidOutDate;
        this.statementDescriptor = builder.statementDescriptor;
        this.status = builder.status;
        this.type = builder.type;
        this.version = builder.version;
    }

    public Long getId() {
        return id;
    }

    public String getGatewayPayoutId() {
        return gatewayPayoutId;
    }

    public Long getAmount() {
        return amount;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public ZonedDateTime getPaidOutDate() {
        return paidOutDate;
    }

    public String getStatementDescriptor() {
        return statementDescriptor;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public Long getVersion() {
        return version;
    }

    public static final class PayoutEntityBuilder {
        private Long id;
        private String gatewayPayoutId;
        private Long amount;
        private ZonedDateTime createdDate;
        private ZonedDateTime paidOutDate;
        private String statementDescriptor;
        private String status;
        private String type;
        private Long version;

        private PayoutEntityBuilder() {
        }

        public static PayoutEntityBuilder aPayoutEntity() {
            return new PayoutEntityBuilder();
        }

        public PayoutEntityBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public PayoutEntityBuilder withGatewayPayoutId(String gatewayPayoutId) {
            this.gatewayPayoutId = gatewayPayoutId;
            return this;
        }

        public PayoutEntityBuilder withAmount(Long amount) {
            this.amount = amount;
            return this;
        }

        public PayoutEntityBuilder withCreatedDate(ZonedDateTime createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public PayoutEntityBuilder withPaidOutDate(ZonedDateTime paidOutDate) {
            this.paidOutDate = paidOutDate;
            return this;
        }

        public PayoutEntityBuilder withStatementDescriptor(String statementDescriptor) {
            this.statementDescriptor = statementDescriptor;
            return this;
        }

        public PayoutEntityBuilder withStatus(String status) {
            this.status = status;
            return this;
        }

        public PayoutEntityBuilder withType(String type) {
            this.type = type;
            return this;
        }

        public PayoutEntityBuilder withVersion(Long version) {
            this.version = version;
            return this;
        }

        public PayoutEntity build() {
            return new PayoutEntity(this);
        }
    }
}
