package uk.gov.pay.ledger.report.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.service.payments.commons.api.json.MicrosecondPrecisionDateTimeSerializer;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TimeseriesReportSlice {

    @JsonSerialize(using = MicrosecondPrecisionDateTimeSerializer.class)
    @Schema(example = "\"2022-04-01T10:26:47.269Z\"")
    private final ZonedDateTime timestamp;
    @Schema(example = "10")
    private final int allPayments;
    @Schema(example = "3")
    private final int erroredPayments;
    @Schema(example = "7")
    private final int completedPayments;
    @Schema(example = "70")
    private final int amount;
    private final int netAmount;
    private final int totalAmount;
    private final int fee;

    public TimeseriesReportSlice(ZonedDateTime timestamp, int allPayments, int erroredPayments, int completedPayments, int amount, int netAmount, int totalAmount, int fee) {
        this.timestamp = timestamp;
        this.allPayments = allPayments;
        this.erroredPayments = erroredPayments;
        this.completedPayments = completedPayments;
        this.amount = amount;
        this.netAmount = netAmount;
        this.totalAmount = totalAmount;
        this.fee = fee;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public int getAllPayments() {
        return allPayments;
    }

    public int getErroredPayments() {
        return erroredPayments;
    }

    public int getCompletedPayments() {
        return completedPayments;
    }

    public int getAmount() {
        return amount;
    }

    public int getNetAmount() {
        return netAmount;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public int getFee() {
        return fee;
    }
}
