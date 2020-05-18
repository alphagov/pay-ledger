package uk.gov.pay.ledger.report.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.commons.api.json.MicrosecondPrecisionDateTimeSerializer;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TimeseriesReportSlice {

    @JsonSerialize(using = MicrosecondPrecisionDateTimeSerializer.class)
    private final ZonedDateTime timestamp;
    private final int allPayments;
    private final int erroredPayments;
    private final int completedPayments;
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
