package uk.gov.pay.ledger.payout.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.commons.api.json.ApiResponseDateTimeSerializer;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.payout.state.PayoutState;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class PayoutView {

    private Long amount;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private ZonedDateTime createdDate;
    private String gatewayPayoutId;
    private String gatewayAccountId;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private ZonedDateTime paidOutDate;
    private PayoutState state;

    private PayoutView(Long amount, ZonedDateTime createdDate, String gatewayPayoutId,
                       String gatewayAccountId, ZonedDateTime paidOutDate, PayoutState state) {
        this.amount = amount;
        this.createdDate = createdDate;
        this.gatewayPayoutId = gatewayPayoutId;
        this.gatewayAccountId = gatewayAccountId;
        this.paidOutDate = paidOutDate;
        this.state = state;
    }

    public static PayoutView from(PayoutEntity payoutEntity) {

        return new PayoutView(payoutEntity.getAmount(), payoutEntity.getCreatedDate(), payoutEntity.getGatewayPayoutId(),
                payoutEntity.getGatewayAccountId(), payoutEntity.getPaidOutDate(), payoutEntity.getState());
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

    public ZonedDateTime getPaidOutDate() {
        return paidOutDate;
    }

    public PayoutState getState() {
        return state;
    }
}
