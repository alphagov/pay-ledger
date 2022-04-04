package uk.gov.pay.ledger.report.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GatewayAccountMonthlyPerformanceReportEntity {

    @Schema(example = "1")
    private final long gatewayAccountId;
    @Schema(example = "1")
    private final long totalVolume;
    @Schema(example = "1000")
    private final BigDecimal totalAmount;
    @Schema(example = "1000.00")
    private final BigDecimal averageAmount;
    @Schema(example = "3")
    private final long month;
    @Schema(example = "2022")
    private final long year;

    public GatewayAccountMonthlyPerformanceReportEntity(long gatewayAccountId,
                                                        long totalVolume,
                                                        BigDecimal totalAmount,
                                                        BigDecimal averageAmount,
                                                        long year,
                                                        long month) {
        this.gatewayAccountId = gatewayAccountId;
        this.totalVolume = totalVolume;
        this.totalAmount = totalAmount;
        this.averageAmount = averageAmount;
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

    public long getYear() {
        return year;
    }

    public long getMonth() {
        return month;
    }
}
