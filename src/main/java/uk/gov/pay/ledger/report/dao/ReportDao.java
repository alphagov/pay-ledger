package uk.gov.pay.ledger.report.dao;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import uk.gov.pay.ledger.report.dao.builder.TransactionStatisticQuery;
import uk.gov.pay.ledger.report.entity.PaymentCountByStateResult;
import uk.gov.pay.ledger.report.entity.TimeseriesReportSlice;
import uk.gov.pay.ledger.report.entity.TransactionsStatisticsResult;
import uk.gov.pay.ledger.report.mapper.ReportMapper;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;

@RegisterRowMapper(ReportMapper.class)
public class ReportDao {
    private static final String COUNT_TRANSACTIONS_BY_STATE = "SELECT state, count(1) AS count FROM transaction t " +
            "WHERE type = :transactionType::transaction_type " +
            ":searchExtraFields " +
            "GROUP BY state";

    private final Jdbi jdbi;

    @Inject
    public ReportDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<PaymentCountByStateResult> getPaymentCountsByState(TransactionStatisticQuery transactionStatisticQuery) {
        return jdbi.withHandle(handle -> {
            String template = createSearchTemplate(transactionStatisticQuery.getFilterTemplates(),
                    COUNT_TRANSACTIONS_BY_STATE);

            Query query = handle.createQuery(template)
                    .bind("transactionType", TransactionType.PAYMENT);
            transactionStatisticQuery.getQueryMap().forEach(query::bind);

            return query.map((rs, rowNum) -> {
                String state = rs.getString("state");
                Long count = rs.getLong("count");
                return new PaymentCountByStateResult(state, count);
            }).list();
        });
    }

    public List<TimeseriesReportSlice> getTransactionsVolumeByTimeseries(ZonedDateTime fromDate, ZonedDateTime toDate) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT " +
                    "date_trunc('hour', t.created_date) as timestamp, " +
                    "COUNT(*) as all_payments, " +
                    "COUNT(*) filter (WHERE t.state IN ('ERROR', 'ERROR_GATEWAY')) as errored_payments, " +
                    "COUNT(*) filter (WHERE t.state IN ('SUCCESS')) as completed_payments, " +
                    "SUM(t.amount) as amount, SUM(t.net_amount) as net_amount, SUM(t.total_amount) as total_amount, SUM(t.fee) as fee " +
                    "FROM transaction t " +
                    "WHERE t.live AND (t.created_date BETWEEN :fromDate AND :toDate) " +
                    "GROUP BY date_trunc('hour', t.created_date) " +
                    "ORDER BY date_trunc('hour', t.created_date)")
                    .bind("fromDate", fromDate)
                    .bind("toDate", toDate)
                    .map(new ReportMapper())
                    .list()
        );
    }

    private String createSearchTemplate(
            List<String> filterTemplates,
            String baseQueryString) {

        String searchClauseTemplate = String.join(" AND ", filterTemplates);
        searchClauseTemplate = StringUtils.isNotBlank(searchClauseTemplate) ?
                "AND " + searchClauseTemplate :
                "";

        return baseQueryString.replace(
                ":searchExtraFields",
                searchClauseTemplate);
    }
}
