package uk.gov.pay.ledger.transaction.search.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.pay.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;

public class SettlementSummary {
    private ZonedDateTime settlementSubmittedTime;
    private ZonedDateTime capturedDate;
    private ZonedDateTime settledDate;

    public SettlementSummary(ZonedDateTime settlementSubmittedTime, ZonedDateTime capturedDate, ZonedDateTime settledDate) {
        this.settlementSubmittedTime = settlementSubmittedTime;
        this.capturedDate = capturedDate;
        this.settledDate = settledDate;
    }

    @JsonProperty("capture_submit_time")
    public Optional<String> getSettlementSubmittedTime() {
        return Optional.ofNullable(settlementSubmittedTime)
                .map(ISO_INSTANT_MILLISECOND_PRECISION::format);
    }

    @JsonProperty("captured_date")
    public Optional<String> getCapturedDate() {
        return Optional.ofNullable(capturedDate)
                .map(t -> t.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    @JsonProperty("settled_date")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Optional<String> getSettledDate() {
        return Optional.ofNullable(settledDate)
                .map(t -> t.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SettlementSummary that = (SettlementSummary) o;

        if (!Objects.equals(settlementSubmittedTime, that.settlementSubmittedTime))
            return false;

        if(!Objects.equals(settledDate, that.settledDate))
            return false;

        return Objects.equals(capturedDate, that.capturedDate);
    }

    @Override
    public int hashCode() {
        int result = settlementSubmittedTime != null ? settlementSubmittedTime.hashCode() : 0;
        result = 31 * result + (capturedDate != null ? capturedDate.hashCode() : 0);
        result = 31 * result + (settledDate != null ? settledDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SettlementSummary{" +
                ", settlementSubmittedTime=" + settlementSubmittedTime +
                ", capturedDate=" + capturedDate +
                '}';
    }
}
