package uk.gov.pay.ledger.payout.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.payout.state.PayoutState;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeSerializer;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class PayoutView {

    private Long amount;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private ZonedDateTime createdDate;
    private String gatewayPayoutId;
    private String gatewayAccountId;
    private String serviceId;
    private Boolean live;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private ZonedDateTime paidOutDate;
    private PayoutState state;

    private PayoutView(Long amount, ZonedDateTime createdDate, String gatewayPayoutId,
                       String gatewayAccountId, String serviceId, Boolean live, ZonedDateTime paidOutDate, PayoutState state) {
        this.amount = amount;
        this.createdDate = createdDate;
        this.gatewayPayoutId = gatewayPayoutId;
        this.gatewayAccountId = gatewayAccountId;
        this.serviceId = serviceId;
        this.live = live;
        this.paidOutDate = paidOutDate;
        this.state = state;
    }

    public static PayoutView from(PayoutEntity payoutEntity) {

        return new PayoutView(payoutEntity.getAmount(), payoutEntity.getCreatedDate(), payoutEntity.getGatewayPayoutId(),
                payoutEntity.getGatewayAccountId(), payoutEntity.getServiceId(), payoutEntity.getLive(), payoutEntity.getPaidOutDate(), payoutEntity.getState());
    }

    public Long getAmount() {
        return amount;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public String getGatewayPayoutId() {
        return gatewayPayoutId;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public Boolean getLive() {
        return live;
    }

    public ZonedDateTime getPaidOutDate() {
        return paidOutDate;
    }

    public PayoutState getState() {
        return state;
    }
}
