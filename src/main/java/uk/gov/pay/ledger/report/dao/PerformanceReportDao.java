package uk.gov.pay.ledger.report.dao;

import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.report.mapper.PerformanceReportEntityMapper;
import uk.gov.pay.ledger.report.entity.PerformanceReportEntity;

import javax.inject.Inject;

public class PerformanceReportDao {

    private static final String PERFORMANCE_REPORT =
            "select coalesce(count(amount),0) as volume, coalesce(sum(amount),0) total_amount, " +
                    "coalesce(avg(amount),0) avg_amount from transaction " +
            "where state='SUCCESS' and type='PAYMENT' AND live=true";

    private final Jdbi jdbi;

    @Inject
    public PerformanceReportDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public PerformanceReportEntity performanceReportForPaymentTransactions() {
        return jdbi.withHandle(handle ->
                handle.createQuery(PERFORMANCE_REPORT)
                        .map(new PerformanceReportEntityMapper()).findOnly());
    }
}
