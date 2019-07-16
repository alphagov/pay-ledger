package uk.gov.pay.ledger.transaction.search.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static uk.gov.pay.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;

public class SettlementSummary {
    private ZonedDateTime captureSubmitTime, capturedTime;

    public SettlementSummary(ZonedDateTime settlementSubmittedTime, ZonedDateTime settledTime) {
        captureSubmitTime = settlementSubmittedTime;
        capturedTime = settledTime;
    }

    public static SettlementSummary ofValue(ZonedDateTime settlementSubmittedTime, ZonedDateTime settledTime) {
        return new SettlementSummary(settlementSubmittedTime, settledTime);
    }

    @JsonProperty("capture_submit_time")
    public String getCaptureSubmitTime() {
        return (captureSubmitTime != null) ? ISO_INSTANT_MILLISECOND_PRECISION.format(captureSubmitTime) : null;
    }

    @JsonProperty("captured_date")
    public String getCapturedDate() {
        return (capturedTime != null) ? capturedTime.format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SettlementSummary that = (SettlementSummary) o;

        if (!Objects.equals(captureSubmitTime, that.captureSubmitTime))
            return false;
        return Objects.equals(capturedTime, that.capturedTime);
    }

    @Override
    public int hashCode() {
        int result = captureSubmitTime != null ? captureSubmitTime.hashCode() : 0;
        result = 31 * result + (capturedTime != null ? capturedTime.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SettlementSummary{" +
                ", captureSubmitTime=" + captureSubmitTime +
                ", capturedTime=" + capturedTime +
                '}';
    }
}
