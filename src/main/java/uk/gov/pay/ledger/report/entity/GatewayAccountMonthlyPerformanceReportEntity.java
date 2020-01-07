package uk.gov.pay.ledger.report.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class GatewayAccountMonthlyPerformanceReportEntity {

    private final long gatewayAccountId;
    private final long totalVolume;
    private final BigDecimal totalAmount;
    private final BigDecimal averageAmount;
    private final BigDecimal minimumAmount;
    private final BigDecimal maximumAmount;
    private final long month;
    private final long year;

    public GatewayAccountMonthlyPerformanceReportEntity(long gatewayAccountId,
                                                        long totalVolume,
                                                        BigDecimal totalAmount,
                                                        BigDecimal averageAmount,
                                                        BigDecimal minimumAmount,
                                                        BigDecimal maximumAmount,
                                                        long year,
                                                        long month) {
        this.gatewayAccountId = gatewayAccountId;
        this.totalVolume = totalVolume;
        this.totalAmount = totalAmount;
        this.averageAmount = averageAmount;
        this.minimumAmount = minimumAmount;
        this.maximumAmount = maximumAmount;
        this.year = year;
        this.month = month;
    }

    public long getGatewayAccountId() {
        return gatewayAccountId;
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

    public BigDecimal getMinimumAmount() {
        return minimumAmount;
    }

    public BigDecimal getMaximumAmount() {
        return maximumAmount;
    }

    public long getYear() {
        return year;
    }

    public long getMonth() {
        return month;
    }
}
