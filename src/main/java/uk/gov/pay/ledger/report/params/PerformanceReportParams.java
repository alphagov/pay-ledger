package uk.gov.pay.ledger.report.params;

import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.util.Optional;

public class PerformanceReportParams {

    private final TransactionState state;

    private PerformanceReportParams(TransactionState state) {
        this.state = state;
    }

    public Optional<TransactionState> getState() {
        return Optional.ofNullable(state);
    }

    public static final class PerformanceReportParamsBuilder {
        private TransactionState state;

        private PerformanceReportParamsBuilder() {
        }

        public static PerformanceReportParamsBuilder builder() {
            return new PerformanceReportParamsBuilder();
        }

        public PerformanceReportParamsBuilder withState(TransactionState state) {
            this.state = state;
            return this;
        }

        public PerformanceReportParams build() {
            return new PerformanceReportParams(state);
        }
    }
}
