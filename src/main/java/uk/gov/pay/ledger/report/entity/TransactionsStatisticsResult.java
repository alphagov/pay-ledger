package uk.gov.pay.ledger.report.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TransactionsStatisticsResult {
    private final long count;
    private final long grossAmount;

    public TransactionsStatisticsResult(long count, long grossAmount) {
        this.count = count;
        this.grossAmount = grossAmount;
    }

    public long getCount() {
        return count;
    }

    public long getGrossAmount() {
        return grossAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionsStatisticsResult that = (TransactionsStatisticsResult) o;
        return count == that.count &&
                grossAmount == that.grossAmount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(count, grossAmount);
    }

    @Override
    public String toString() {
        return "TransactionsStatisticsResult{" +
                "count=" + count +
                ", grossAmount=" + grossAmount +
                '}';
    }
}
