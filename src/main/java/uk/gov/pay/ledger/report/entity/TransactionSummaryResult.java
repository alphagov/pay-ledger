package uk.gov.pay.ledger.report.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TransactionSummaryResult {

    private TransactionsStatisticsResult payments;
    private TransactionsStatisticsResult motoPayments;
    private TransactionsStatisticsResult refunds;
    @Schema(example = "6000")
    private Long netIncome;

    public TransactionSummaryResult(TransactionsStatisticsResult payments,
                                    TransactionsStatisticsResult motoPayments,
                                    TransactionsStatisticsResult refunds,
                                    Long netIncome) {
        this.payments = payments;
        this.motoPayments = motoPayments;
        this.refunds = refunds;
        this.netIncome = netIncome;
    }

    public TransactionsStatisticsResult getPayments() {
        return payments;
    }

    public TransactionsStatisticsResult getMotoPayments() {
        return motoPayments;
    }

    public TransactionsStatisticsResult getRefunds() {
        return refunds;
    }

    public Long getNetIncome() {
        return netIncome;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        TransactionSummaryResult that = (TransactionSummaryResult) o;
        return that.payments.equals(this.payments) &&
                that.motoPayments.equals(this.motoPayments) &&
                that.refunds.equals(this.refunds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payments.hashCode(), motoPayments.hashCode(), refunds.hashCode(), netIncome);
    }

    @Override
    public String toString() {
        return "TransactionsSummaryResult: { payments: " + payments.toString() +
                " moto payments: " + motoPayments.toString() +
                " refunds: " + refunds.toString() +
                " total in pence: " + netIncome +
                " }";
    }
}
