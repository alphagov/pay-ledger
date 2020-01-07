package uk.gov.pay.ledger.report.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import uk.gov.pay.ledger.report.entity.GatewayAccountMonthlyPerformanceReportEntity;
import uk.gov.pay.ledger.report.entity.PerformanceReportEntity;
import uk.gov.pay.ledger.report.mapper.GatewayAccountMonthlyPerformanceReportEntityMapper;
import uk.gov.pay.ledger.report.mapper.PerformanceReportEntityMapper;
import uk.gov.pay.ledger.report.params.PerformanceReportParams;

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

    private static final String MONTHLY_GATEWAY_ACCOUNT_PERFORMANCE_STATISTICS = "SELECT " +
            "t.gateway_account_id, " +
            "COALESCE(COUNT(t.amount), 0) AS volume, " +
            "COALESCE(SUM(t.amount), 0) AS total_amount, " +
            "COALESCE(AVG(t.amount), 0) AS avg_amount, " +
            "COALESCE(MIN(t.amount), 0) AS min_amount, " +
            "COALESCE(MAX(t.amount), 0) AS max_amount, " +
            "EXTRACT(YEAR from created_date) AS year, " +
            "EXTRACT(MONTH from created_date) AS month " +
            "FROM transaction t " +
            "WHERE t.state = 'SUCCESS' " +
            "AND t.type = 'PAYMENT' " +
            "AND t.live = TRUE " +
            "AND created_date BETWEEN :startDate AND :endDate " +
            "GROUP BY t.gateway_account_id, year, month " +
            "ORDER BY t.gateway_account_id, year, month";

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

    public List<GatewayAccountMonthlyPerformanceReportEntity> monthlyPerformanceReportForGatewayAccounts(ZonedDateTime startDate, ZonedDateTime endDate) {
        return jdbi.withHandle(handle ->
                handle
                        .createQuery(MONTHLY_GATEWAY_ACCOUNT_PERFORMANCE_STATISTICS)
                        .bind("startDate", startDate)
                        .bind("endDate", endDate)
                        .map(new GatewayAccountMonthlyPerformanceReportEntityMapper()).list());
    }
}
