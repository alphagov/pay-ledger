package uk.gov.pay.ledger.transaction.search.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

public class RefundSettlementSummary {
    private ZonedDateTime settledDate;

    public RefundSettlementSummary(ZonedDateTime settledDate) {
        this.settledDate = settledDate;
    }

    @JsonProperty("settled_date")
    public Optional<String> getSettledDate() {
        return Optional.ofNullable(settledDate)
                .map(t -> t.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefundSettlementSummary that = (RefundSettlementSummary) o;

        return Objects.equals(settledDate, that.settledDate);
    }

    @Override
    public int hashCode() {
        int result = 31 * (settledDate != null ? settledDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RefundSettlementSummary{" +
                "settledDate=" + settledDate +
                '}';
    }
}
