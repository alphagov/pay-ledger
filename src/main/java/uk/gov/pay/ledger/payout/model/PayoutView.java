package uk.gov.pay.ledger.payout.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.payout.state.PayoutState;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeSerializer;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PayoutView {

    @Schema(description = "amount in pence", example = "1000")
    private Long amount;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    @Schema(implementation = ZonedDateTime.class, example = "\"2022-03-30T15:09:20.241Z\"")
    private ZonedDateTime createdDate;
    @Schema(example = "po_sd0fkpam0dlwe10dmlsl3mf")
    private String gatewayPayoutId;
    @Schema(example = "1")
    private String gatewayAccountId;
    @Schema(example = "dlsd0dkad20sk0adne9fjd")
    private String serviceId;
    @Schema(example = "true")
    private Boolean live;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private ZonedDateTime paidOutDate;
    @Schema(implementation = PayoutState.class, type="object", example = "{\"status\": \"paidout\",\"finished\": true }")
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
