package uk.gov.pay.ledger.report.entity;

import java.math.BigDecimal;

public class PerformanceReportEntity {
    private final long totalVolume;
    private final BigDecimal totalAmount;
    private final BigDecimal averageAmount;

    public PerformanceReportEntity(long totalVolume, BigDecimal totalAmount, BigDecimal averageAmount) {
        this.totalVolume = totalVolume;
        this.totalAmount = totalAmount;
        this.averageAmount = averageAmount;
    }

    public long getTotalVolume() {
        return totalVolume;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getAverageAmount() {
        return averageAmount;
    }
}
