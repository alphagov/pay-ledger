package uk.gov.pay.ledger.report.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import uk.gov.pay.ledger.report.entity.GatewayAccountMonthlyPerformanceReportEntity;
import uk.gov.pay.ledger.report.entity.PerformanceReportEntity;
import uk.gov.pay.ledger.report.mapper.GatewayAccountMonthlyPerformanceReportEntityMapper;
import uk.gov.pay.ledger.report.mapper.PerformanceReportEntityMapper;
import uk.gov.pay.ledger.report.params.PerformanceReportParams;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;

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

    public PerformanceReportEntity performanceReportForPaymentTransactions(String fromDate, String toDate, String state) {
        StringBuilder queryString = new StringBuilder(PERFORMANCE_REPORT);

        if (fromDate != null && toDate != null) queryString.append(WITH_DATE_RANGE);
        if (state != null) queryString.append(WITH_STATE);

        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(queryString.toString());
            if (state != null) query.bind("state", TransactionState.from(state).name());
            if (fromDate != null && toDate != null) {
                query.bind("startDate", ZonedDateTime.parse(fromDate));
                query.bind("toDate", ZonedDateTime.parse(toDate));
            }
            return query.map(new PerformanceReportEntityMapper()).one();
        });
    }
}
