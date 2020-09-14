package uk.gov.pay.ledger.transaction.search.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

public class SettlementSummary {

    protected ZonedDateTime settledDate;

    public SettlementSummary(ZonedDateTime settledDate) {
        this.settledDate = settledDate;
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

        return Objects.equals(settledDate, that.settledDate);
    }

    @Override
    public int hashCode() {
        return  31 * (settledDate != null ? settledDate.hashCode() : 0);
    }

    @Override
    public String toString() {
        return "SettlementSummary{" +
                ", settledDate=" + settledDate +
                '}';
    }
}
