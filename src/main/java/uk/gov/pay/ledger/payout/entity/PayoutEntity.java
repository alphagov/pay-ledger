package uk.gov.pay.ledger.payout.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.service.payments.commons.api.json.MicrosecondPrecisionDateTimeDeserializer;
import uk.gov.service.payments.commons.api.json.MicrosecondPrecisionDateTimeSerializer;
import uk.gov.pay.ledger.payout.state.PayoutState;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class PayoutEntity {

    @JsonIgnore
    private Long id;
    private String gatewayPayoutId;
    private String serviceId;
    private Boolean live;
    private Long amount;
    @JsonIgnore
    private ZonedDateTime createdDate;
    @JsonSerialize(using = MicrosecondPrecisionDateTimeSerializer.class)
    @JsonDeserialize(using = MicrosecondPrecisionDateTimeDeserializer.class)
    private ZonedDateTime paidOutDate;
    private PayoutState state;
    private Integer eventCount;
    private String payoutDetails;
    private String gatewayAccountId;

    public PayoutEntity() {
    }

    public PayoutEntity(PayoutEntityBuilder builder) {
        this.id = builder.id;
        this.gatewayPayoutId = builder.gatewayPayoutId;
        this.serviceId = builder.serviceId;
        this.live = builder.live;
        this.amount = builder.amount;
        this.createdDate = builder.createdDate;
        this.paidOutDate = builder.paidOutDate;
        this.state = builder.state;
        this.eventCount = builder.eventCount;
        this.payoutDetails = builder.payoutDetails;
        this.gatewayAccountId = builder.gatewayAccountId;
    }

    public Long getId() {
        return id;
    }

    public String getGatewayPayoutId() {
        return gatewayPayoutId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public boolean isLive() {
        return live;
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

    public PayoutState getState() {
        return state;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public String getPayoutDetails() {
        return payoutDetails;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public void setGatewayPayoutId(String gatewayPayoutId) {
        this.gatewayPayoutId = gatewayPayoutId;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setPaidOutDate(ZonedDateTime paidOutDate) {
        this.paidOutDate = paidOutDate;
    }

    public void setState(PayoutState state) {
        this.state = state;
    }

    public void setPayoutDetails(String payoutDetails) {
        this.payoutDetails = payoutDetails;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }

    public PayoutEntity setGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
        return this;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setLive(Boolean live) {
        this.live = live;
    }

    public static final class PayoutEntityBuilder {
        private Long id;
        private String gatewayPayoutId;
        private String serviceId;
        private Boolean live;
        private Long amount;
        private ZonedDateTime createdDate;
        private ZonedDateTime paidOutDate;
        private PayoutState state;
        private Integer eventCount;
        private String payoutDetails;
        private String gatewayAccountId;

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

        public PayoutEntityBuilder withServiceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public PayoutEntityBuilder withLive(Boolean live) {
            this.live = live;
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

        public PayoutEntityBuilder withState(PayoutState state) {
            this.state = state;
            return this;
        }

        public PayoutEntityBuilder withEventCount(Integer eventCount) {
            this.eventCount = eventCount;
            return this;
        }

        public PayoutEntityBuilder withGatewayAccountId(String gatewayAccountId) {
            this.gatewayAccountId = gatewayAccountId;
            return this;
        }

        public PayoutEntityBuilder withPayoutDetails(String payoutDetails) {
            this.payoutDetails = payoutDetails;
            return this;
        }

        public PayoutEntity build() {
            return new PayoutEntity(this);
        }
    }
}
