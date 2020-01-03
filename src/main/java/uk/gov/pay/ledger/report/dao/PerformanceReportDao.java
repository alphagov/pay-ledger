package uk.gov.pay.ledger.report.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import uk.gov.pay.ledger.report.entity.PerformanceReportEntity;
import uk.gov.pay.ledger.report.mapper.PerformanceReportEntityMapper;
import uk.gov.pay.ledger.report.params.PerformanceReportParams;

import javax.inject.Inject;

public class PerformanceReportDao {

    private static final String PERFORMANCE_REPORT =
            "select coalesce(count(amount),0) as volume, coalesce(sum(amount),0) total_amount, " +
                    "coalesce(avg(amount),0) avg_amount from transaction " +
            "where type='PAYMENT' and live=true";

    private static final String WITH_STATE = " and state=:state";

    private static final String WITH_DATE_RANGE = " and created_date between :startDate and :toDate";

    private final Jdbi jdbi;

    @Inject
    public PerformanceReportDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public PerformanceReportEntity performanceReportForPaymentTransactions(PerformanceReportParams params) {
        StringBuilder queryString = new StringBuilder(PERFORMANCE_REPORT);
        params.getState().ifPresent(state -> queryString.append(WITH_STATE));
        params.getDateRange().ifPresent(dateRange -> queryString.append(WITH_DATE_RANGE));

        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(queryString.toString());
            params.getState().ifPresent(state -> query.bind("state", state.name()));
            params.getDateRange().ifPresent(dateRange -> {
                query.bind("startDate", dateRange.getFromDate());
                query.bind("toDate", dateRange.getToDate());
            });
            return query.map(new PerformanceReportEntityMapper()).findOnly();
        });
    }
}
