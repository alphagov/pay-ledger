package uk.gov.pay.ledger.transactionsummary.dao;

import com.google.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import uk.gov.pay.ledger.report.entity.GatewayAccountMonthlyPerformanceReportEntity;
import uk.gov.pay.ledger.report.entity.PerformanceReportEntity;
import uk.gov.pay.ledger.report.mapper.GatewayAccountMonthlyPerformanceReportEntityMapper;
import uk.gov.pay.ledger.report.mapper.PerformanceReportEntityMapper;
import uk.gov.pay.ledger.report.params.PerformanceReportParams;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public class TransactionSummaryDao {

    private static final String UPSERT_STRING = "INSERT INTO transaction_summary AS ts(gateway_account_id, type, " +
            " transaction_date, state, live, moto, total_amount_in_pence, no_of_transactions, total_fee_in_pence)" +
            " VALUES(:gatewayAccountId, :type, :transactionDate, :state, :live, :moto, :amountInPence, 1,0)" +
            " ON CONFLICT ON CONSTRAINT transaction_summmary_unique_key " +
            " DO UPDATE SET no_of_transactions = ts.no_of_transactions + 1, " +
            " total_amount_in_pence = :amountInPence + ts.total_amount_in_pence" +
            " WHERE ts.gateway_account_id = :gatewayAccountId " +
            " AND ts.transaction_date = :transactionDate " +
            " AND ts.state = :state " +
            " AND ts.type = :type" +
            " AND ts.live = :live" +
            " AND ts.moto = :moto";

    private static final String DEDUCT_TRANSACTION_SUMMARY = "UPDATE transaction_summary ts " +
            " SET no_of_transactions = ts.no_of_transactions - 1, " +
            " total_amount_in_pence = ts.total_amount_in_pence - :amountInPence, " +
            " total_fee_in_pence = ts.total_fee_in_pence - :feeInPence " +
            " WHERE ts.gateway_account_id = :gatewayAccountId " +
            " AND ts.transaction_date = :transactionDate " +
            " AND ts.state = :state " +
            " AND ts.type = :type" +
            " AND ts.live = :live" +
            " AND ts.moto = :moto";

    private static final String UPDATE_FEE = "UPDATE transaction_summary ts " +
            " SET total_fee_in_pence = ts.total_fee_in_pence + :feeInPence " +
            " WHERE ts.gateway_account_id = :gatewayAccountId " +
            " AND ts.transaction_date = :transactionDate " +
            " AND ts.state = :state " +
            " AND ts.type = :type" +
            " AND ts.live = :live" +
            " AND ts.moto = :moto";

    private static final String MONTHLY_GATEWAY_ACCOUNT_PERFORMANCE_STATISTICS = "SELECT " + "t.gateway_account_id, " +
            "COALESCE(SUM(t.no_of_transactions), 0) AS volume, " +
            "COALESCE(SUM(t.total_amount_in_pence), 0) AS total_amount, " +
            "COALESCE(SUM(t.total_amount_in_pence)/SUM(t.no_of_transactions), 0) AS avg_amount, " +
            "EXTRACT(YEAR from transaction_date) AS year, " +
            "EXTRACT(MONTH from transaction_date) AS month " +
            "FROM transaction_summary t " +
            "WHERE t.state = 'SUCCESS' " +
            "AND t.type = 'PAYMENT' " +
            "AND t.live = TRUE " +
            "AND transaction_date BETWEEN :startDate AND :endDate " +
            "GROUP BY t.gateway_account_id, year, month " +
            "ORDER BY t.gateway_account_id, year, month";

    private static final String PERFORMANCE_REPORT_QUERY = "SELECT COALESCE(SUM(t.no_of_transactions),0) AS volume, " +
            "COALESCE(SUM(t.total_amount_in_pence), 0) AS total_amount, " +
            "COALESCE(SUM(t.total_amount_in_pence)/SUM(t.no_of_transactions), 0) AS avg_amount " +
            "FROM transaction_summary t " +
            "WHERE t.type= 'PAYMENT' " +
            "AND t.live= TRUE";

    private static final String WITH_STATE = " AND t.state=:state";

    private static final String WITH_DATE_RANGE = " AND t.transaction_date BETWEEN :startDate AND :toDate";

    private final Jdbi jdbi;

    @Inject
    public TransactionSummaryDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void upsert(String gatewayAccountId, String transactionType, LocalDate transactionDate,
                       TransactionState state, boolean live, boolean moto, Long amount) {
        jdbi.withHandle(handle ->
                handle.createUpdate(UPSERT_STRING)
                        .bind("gatewayAccountId", gatewayAccountId)
                        .bind("type", transactionType)
                        .bind("transactionDate", transactionDate)
                        .bind("state", state)
                        .bind("live", live)
                        .bind("moto", moto)
                        .bind("amountInPence", amount)
                        .execute()
        );
    }

    public void updateFee(String gatewayAccountId, String transactionType, LocalDate transactionDate,
                          TransactionState state, boolean live, boolean moto, Long fee) {
        jdbi.withHandle(handle ->
                handle.createUpdate(UPDATE_FEE)
                        .bind("gatewayAccountId", gatewayAccountId)
                        .bind("type", transactionType)
                        .bind("transactionDate", transactionDate)
                        .bind("state", state)
                        .bind("live", live)
                        .bind("moto", moto)
                        .bind("feeInPence", fee)
                        .execute()
        );
    }

    public void deductTransactionSummaryFor(String gatewayAccountId, String transactionType,
                                            LocalDate transactionDate, TransactionState state, boolean live,
                                            boolean moto, Long amount, Long fee) {
        jdbi.withHandle(handle ->
                handle.createUpdate(DEDUCT_TRANSACTION_SUMMARY)
                        .bind("gatewayAccountId", gatewayAccountId)
                        .bind("type", transactionType)
                        .bind("transactionDate", transactionDate)
                        .bind("state", state)
                        .bind("live", live)
                        .bind("moto", moto)
                        .bind("amountInPence", amount)
                        .bind("feeInPence", fee)
                        .execute()
        );
    }

    public List<GatewayAccountMonthlyPerformanceReportEntity> monthlyPerformanceReportForGatewayAccounts(LocalDate startDate, LocalDate endDate) {
        return jdbi.withHandle(handle ->
                handle
                        .createQuery(MONTHLY_GATEWAY_ACCOUNT_PERFORMANCE_STATISTICS)
                        .bind("startDate", startDate)
                        .bind("endDate", endDate)
                        .map(new GatewayAccountMonthlyPerformanceReportEntityMapper()).list());
    }

    public PerformanceReportEntity performanceReportForPaymentTransactions(PerformanceReportParams params) {
        StringBuilder queryString = new StringBuilder(PERFORMANCE_REPORT_QUERY);
        params.getState().ifPresent(state -> queryString.append(WITH_STATE));
        params.getDateRange().ifPresent(dateRange -> queryString.append(WITH_DATE_RANGE));

        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(queryString.toString());
            params.getState().ifPresent(state -> query.bind("state", state.name()));
            params.getDateRange().ifPresent(dateRange -> {
                query.bind("startDate", dateRange.getFromDate());
                query.bind("toDate", dateRange.getToDate());
            });
            return query.map(new PerformanceReportEntityMapper()).one();
        });
    }
}
