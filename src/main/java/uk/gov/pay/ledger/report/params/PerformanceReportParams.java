package uk.gov.pay.ledger.report.params;

import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;
import java.util.Optional;

public class PerformanceReportParams {

    private final TransactionState state;
    private final ZonedDateTime fromDate;
    private final ZonedDateTime toDate;

    private PerformanceReportParams(TransactionState state, ZonedDateTime fromDate, ZonedDateTime toDate) {
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
        private ZonedDateTime fromDate;
        private ZonedDateTime toDate;

        private PerformanceReportParamsBuilder() {
        }

        public static PerformanceReportParamsBuilder builder() {
            return new PerformanceReportParamsBuilder();
        }

        public PerformanceReportParamsBuilder withState(TransactionState state) {
            this.state = state;
            return this;
        }

        public PerformanceReportParamsBuilder withFromDate(ZonedDateTime fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public PerformanceReportParamsBuilder withToDate(ZonedDateTime toDate) {
            this.toDate = toDate;
            return this;
        }

        public PerformanceReportParams build() {
            return new PerformanceReportParams(state, fromDate, toDate);
        }
    }

    public static final class DateRange {
        private final ZonedDateTime fromDate;
        private final ZonedDateTime toDate;

        public DateRange(ZonedDateTime fromDate, ZonedDateTime toDate) {
            this.fromDate = fromDate;
            this.toDate = toDate;
        }

        public ZonedDateTime getFromDate() {
            return fromDate;
        }

        public ZonedDateTime getToDate() {
            return toDate;
        }
    }
}
