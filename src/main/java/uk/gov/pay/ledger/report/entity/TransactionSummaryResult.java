package uk.gov.pay.ledger.report.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TransactionSummaryResult {

    private TransactionsStatisticsResult payments;
    private TransactionsStatisticsResult refunds;
    private Long netIncome;

    public TransactionSummaryResult(TransactionsStatisticsResult payments,
                                    TransactionsStatisticsResult refunds,
                                    Long netIncome) {
        this.payments = payments;
        this.refunds = refunds;
        this.netIncome = netIncome;
    }

    public TransactionsStatisticsResult getPayments() {
        return payments;
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
                that.refunds.equals(this.refunds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payments.hashCode(), refunds.hashCode(), netIncome);
    }

    @Override
    public String toString() {
        return "TransactionsSummaryResult: { payments: " + payments.toString() +
                " refunds: " + refunds.toString() +
                " total in pence: " + netIncome +
                " }";
    }
}
