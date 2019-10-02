package uk.gov.pay.ledger.report.entity;

import java.util.Objects;

public class PaymentCountByStateResult {
    private final String state;
    private final Long count;

    public PaymentCountByStateResult(String state, Long count) {
        this.state = state;
        this.count = count;
    }

    public String getState() {
        return state;
    }

    public Long getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentCountByStateResult that = (PaymentCountByStateResult) o;
        return Objects.equals(state, that.state) &&
                Objects.equals(count, that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, count);
    }

    @Override
    public String toString() {
        return "PaymentCountByStateResult{" +
                "state='" + state + '\'' +
                ", count=" + count +
                '}';
    }
}
