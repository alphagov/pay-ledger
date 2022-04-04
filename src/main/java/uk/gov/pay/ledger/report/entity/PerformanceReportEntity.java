package uk.gov.pay.ledger.report.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PerformanceReportEntity {

    @Schema(example = "1")
    private final long totalVolume;

    @Schema(example = "1000")
    private final BigDecimal totalAmount;

    @Schema(example = "1000.00")
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
