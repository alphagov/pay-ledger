package uk.gov.pay.ledger.transactionsummary.dao;

import com.google.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;

public class TransactionSummaryDao {

    private static final String UPSERT_STRING = "INSERT INTO transaction_summary AS ts(gateway_account_id, " +
            " transaction_date, state, live, total_amount_in_pence, no_of_transactions) " +
            " VALUES(:gatewayAccountId, :transactionDate, :state, :live, :amountInPence, 1) " +
            " ON CONFLICT ON CONSTRAINT transaction_summmary_unique_key " +
            " DO UPDATE SET no_of_transactions = ts.no_of_transactions + 1, " +
            " total_amount_in_pence = :amountInPence + ts.total_amount_in_pence" +
            " WHERE ts.gateway_account_id = :gatewayAccountId " +
            " AND ts.transaction_date = :transactionDate " +
            " AND ts.state = :state " +
            " AND ts.live = :live";

    private static final String UPDATE_SUMMARY = "UPDATE transaction_summary ts " +
            " SET no_of_transactions = ts.no_of_transactions - 1, " +
            " total_amount_in_pence = ts.total_amount_in_pence - :amountInPence " +
            " WHERE ts.gateway_account_id = :gatewayAccountId " +
            " AND ts.transaction_date = :transactionDate " +
            " AND ts.state = :state " +
            " AND ts.live = :live";

    private final Jdbi jdbi;

    @Inject
    public TransactionSummaryDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void upsert(String gatewayAccountId, ZonedDateTime transactionDate, TransactionState state, Boolean live, Long amount) {
        jdbi.withHandle(handle ->
                handle.createUpdate(UPSERT_STRING)
                        .bind("gatewayAccountId", gatewayAccountId)
                        .bind("transactionDate", transactionDate)
                        .bind("state", state)
                        .bind("live", live)
                        .bind("amountInPence", amount)
                        .execute()
        );
    }

    public void updateSummary(String gatewayAccountId, ZonedDateTime transactionDate, TransactionState state,
                              boolean live, Long amount) {
        jdbi.withHandle(handle ->
                handle.createUpdate(UPDATE_SUMMARY)
                        .bind("gatewayAccountId", gatewayAccountId)
                        .bind("transactionDate", transactionDate)
                        .bind("state", state)
                        .bind("live", live)
                        .bind("amountInPence", amount)
                        .execute()
        );
    }
}
