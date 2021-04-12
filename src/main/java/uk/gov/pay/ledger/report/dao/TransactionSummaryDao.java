package uk.gov.pay.ledger.report.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import uk.gov.pay.ledger.report.mapper.ReportMapper;

import javax.inject.Inject;

public class TransactionSummaryDao {

    private final Jdbi jdbi;

    private static final String UPDATE_TRANSACTION_SUMMARY = "INSERT INTO transaction_summary" +
            " (gateway_account_id, transaction_date, state, live, total_amount_in_pence, no_of_transactions)" +
            " SELECT  gateway_account_id,  date_trunc('day', created_date)," +
            "    state, live, sum(amount) total_amount_in_pence, count(id) no_of_transactions" +
            " FROM   TRANSACTION" +
            " WHERE  created_date < CURRENT_DATE" +
            " AND    created_date >= (SELECT last_refresh_date FROM   transaction_summary_refresh_status)" +
            " GROUP BY gateway_account_id, date_trunc('day', created_date), state, live" +
            " ORDER BY gateway_account_id, date_trunc('day', created_date)";

    private static final String UPDATE_TRANSACTION_SUMMARY_REFRESH_DATE = "update transaction_summary_refresh_status" +
            " set last_refresh_date = CURRENT_DATE";

    @Inject
    public TransactionSummaryDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void updateTransactionSummary() {
        jdbi.withHandle(handle ->
                handle.createUpdate(UPDATE_TRANSACTION_SUMMARY)
                        .execute());
    }

    public void updateLastRefreshData() {
        jdbi.withHandle(handle ->
                handle.createUpdate(UPDATE_TRANSACTION_SUMMARY_REFRESH_DATE)
                        .execute());
    }
}
