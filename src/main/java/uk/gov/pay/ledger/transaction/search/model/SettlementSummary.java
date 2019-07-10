package uk.gov.pay.ledger.transaction.search.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static uk.gov.pay.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;

public class SettlementSummary {
    private ZonedDateTime captureSubmitTime, capturedTime;

    public void setCaptureSubmitTime(ZonedDateTime captureSubmitTime) {
        this.captureSubmitTime = captureSubmitTime;
    }

    @JsonProperty("capture_submit_time")
    public String getCaptureSubmitTime() {
        return (captureSubmitTime != null) ? ISO_INSTANT_MILLISECOND_PRECISION.format(captureSubmitTime) : null;
    }

    public void setCapturedTime(ZonedDateTime capturedTime) {
        this.capturedTime = capturedTime;
    }

    @JsonProperty("captured_date")
    public String getCapturedDate() {
        return (capturedTime != null) ? capturedTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ISO_DATE) : null;
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
