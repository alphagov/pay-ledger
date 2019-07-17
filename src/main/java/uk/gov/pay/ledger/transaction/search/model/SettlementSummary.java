package uk.gov.pay.ledger.transaction.search.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.pay.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;

public class SettlementSummary {
    private ZonedDateTime settlementSubmittedTime;
    private ZonedDateTime settledTime;

    public SettlementSummary(ZonedDateTime settlementSubmittedTime, ZonedDateTime settledTime) {
        this.settlementSubmittedTime = settlementSubmittedTime;
        this.settledTime = settledTime;
    }

    @JsonProperty("capture_submit_time")
    public Optional<String> getSettlementSubmittedTime() {
        return Optional.ofNullable(settlementSubmittedTime)
                .map(t -> ISO_INSTANT_MILLISECOND_PRECISION.format(t));
    }

    @JsonProperty("captured_date")
    public Optional<String> getCapturedDate() {
        return Optional.ofNullable(settledTime)
                .map(t -> t.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SettlementSummary that = (SettlementSummary) o;

        if (!Objects.equals(settlementSubmittedTime, that.settlementSubmittedTime))
            return false;
        return Objects.equals(settledTime, that.settledTime);
    }

    @Override
    public int hashCode() {
        int result = settlementSubmittedTime != null ? settlementSubmittedTime.hashCode() : 0;
        result = 31 * result + (settledTime != null ? settledTime.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SettlementSummary{" +
                ", settlementSubmittedTime=" + settlementSubmittedTime +
                ", settledTime=" + settledTime +
                '}';
    }
}
