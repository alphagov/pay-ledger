package uk.gov.pay.ledger.report.params;

import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.LocalDate;
import java.util.Optional;

public class PerformanceReportParams {

    private final TransactionState state;
    private final LocalDate fromDate;
    private final LocalDate toDate;

    private PerformanceReportParams(TransactionState state, LocalDate fromDate, LocalDate toDate) {
        this.state = state;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public Optional<TransactionState> getState() {
        return Optional.ofNullable(state);
    }

    public Optional<DateRange> getDateRange() {
        if (fromDate != null && toDate !=null) {
            return Optional.of(new DateRange(fromDate, toDate));
        }
        return Optional.empty();
    }

    public static final class PerformanceReportParamsBuilder {
        private TransactionState state;
        private LocalDate fromDate;
        private LocalDate toDate;

        private PerformanceReportParamsBuilder() {
        }

        public static PerformanceReportParamsBuilder builder() {
            return new PerformanceReportParamsBuilder();
        }

        public PerformanceReportParamsBuilder withState(TransactionState state) {
            this.state = state;
            return this;
        }

        public PerformanceReportParamsBuilder withFromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public PerformanceReportParamsBuilder withToDate(LocalDate toDate) {
            this.toDate = toDate;
            return this;
        }

        public PerformanceReportParams build() {
            return new PerformanceReportParams(state, fromDate, toDate);
        }
    }

    public static final class DateRange {
        private final LocalDate fromDate;
        private final LocalDate toDate;

        public DateRange(LocalDate fromDate, LocalDate toDate) {
            this.fromDate = fromDate;
            this.toDate = toDate;
        }

        public LocalDate getFromDate() {
            return fromDate;
        }

        public LocalDate getToDate() {
            return toDate;
        }
    }
}
