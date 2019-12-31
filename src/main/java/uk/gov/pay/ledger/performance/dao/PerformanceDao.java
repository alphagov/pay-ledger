package uk.gov.pay.ledger.performance.dao;

import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.performance.dao.mapper.PerformanceReportEntityMapper;
import uk.gov.pay.ledger.performance.entity.PerformanceReportEntity;

import javax.inject.Inject;

public class PerformanceDao {

    private static final String PERFORMANCE_REPORT =
            "select coalesce(count(amount),0) as volume, coalesce(sum(amount),0) total_amount, " +
                    "coalesce(avg(amount),0) avg_amount from transaction " +
            "where state='SUCCESS' and type='PAYMENT' AND live=true";

    private final Jdbi jdbi;

    @Inject
    public PerformanceDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public PerformanceReportEntity performanceReportForPaymentTransactions() {
        return jdbi.withHandle(handle ->
                handle.createQuery(PERFORMANCE_REPORT)
                        .map(new PerformanceReportEntityMapper()).findOnly());
    }
}
