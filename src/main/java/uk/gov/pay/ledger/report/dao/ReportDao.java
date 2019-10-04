package uk.gov.pay.ledger.report.dao;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import uk.gov.pay.ledger.report.entity.PaymentCountByStateResult;
import uk.gov.pay.ledger.report.entity.PaymentsStatisticsResult;
import uk.gov.pay.ledger.report.params.PaymentsReportParams;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import javax.inject.Inject;
import java.util.List;

public class ReportDao {
    private static final String COUNT_TRANSACTIONS_BY_STATE = "SELECT state, count(1) AS count FROM transaction t " +
            "WHERE type = :transactionType::transaction_type " +
            ":searchExtraFields " +
            "GROUP BY state";

    private static final String PAYMENT_STATISTICS = "SELECT count(1) AS count, sum(total_amount) AS grossAmount FROM transaction t " +
            "WHERE type = :transactionType::transaction_type " +
            "AND state = :state " +
            ":searchExtraFields ";

    private final Jdbi jdbi;

    @Inject
    public ReportDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<PaymentCountByStateResult> getPaymentCountsByState(PaymentsReportParams params) {
        return jdbi.withHandle(handle -> {
            String template = createSearchTemplate(params.getFilterTemplates(),
                    COUNT_TRANSACTIONS_BY_STATE);

            Query query = handle.createQuery(template)
                    .bind("transactionType", TransactionType.PAYMENT);
            params.getQueryMap().forEach(query::bind);

            return query.map((rs, rowNum) -> {
                String state = rs.getString("state");
                Long count = rs.getLong("count");
                return new PaymentCountByStateResult(state, count);
            }).list();
        });
    }

    public PaymentsStatisticsResult getPaymentsStatistics(PaymentsReportParams params) {
        return jdbi.withHandle(handle -> {
            String template = createSearchTemplate(params.getFilterTemplates(),
                    PAYMENT_STATISTICS);

            Query query = handle.createQuery(template)
                    .bind("transactionType", TransactionType.PAYMENT)
                    .bind("state", TransactionState.SUCCESS);
            params.getQueryMap().forEach(query::bind);

            return query.map((rs, rowNum) -> {
                long count = rs.getLong("count");
                long grossAmount = rs.getLong("grossAmount");
                return new PaymentsStatisticsResult(count, grossAmount);
            }).findOnly();
        });
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
