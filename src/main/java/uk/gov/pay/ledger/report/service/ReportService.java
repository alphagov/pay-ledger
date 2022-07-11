package uk.gov.pay.ledger.report.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.report.dao.ReportDao;

import uk.gov.pay.ledger.report.dao.builder.TransactionStatisticQuery;
import uk.gov.pay.ledger.report.entity.TimeseriesReportSlice;
import uk.gov.pay.ledger.report.entity.TransactionsStatisticsResult;
import uk.gov.pay.ledger.report.entity.TransactionSummaryResult;
import uk.gov.pay.ledger.report.params.TransactionSummaryParams;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ReportService {

    private final ReportDao reportDao;

    @Inject
    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public Map<String, Long> getPaymentCountsByState(TransactionSummaryParams params) {
        // return map with all states, with count of 0 if no payments exist for state
        Map<String, Long> responseMap = new HashMap<>();
        TransactionState.PAYMENT_TRANSACTION_STATES.forEach(state -> responseMap.put(state.getStatus() , 0L));

        TransactionStatisticQuery transactionStatisticQuery = buildBaseTransactionStatisticQuery(params);

        reportDao.getPaymentCountsByState(transactionStatisticQuery).forEach(result ->
                responseMap.put(TransactionState.from(result.getState()).getStatus(), result.getCount()));

        return responseMap;
    }

    public TransactionSummaryResult getTransactionsSummary(TransactionSummaryParams params) {
        TransactionStatisticQuery transactionStatisticQuery = buildBaseTransactionStatisticQuery(params);

        TransactionsStatisticsResult payments = reportDao.getTransactionSummaryStatistics(transactionStatisticQuery, TransactionType.PAYMENT);
        TransactionsStatisticsResult refunds = reportDao.getTransactionSummaryStatistics(transactionStatisticQuery, TransactionType.REFUND);
        TransactionsStatisticsResult motoPayments = new TransactionsStatisticsResult(0L, 0L);

        if (params.isIncludeMotoStatistics()) {
            TransactionStatisticQuery motoQuery = transactionStatisticQuery.withMoto(true);
            motoPayments = reportDao.getTransactionSummaryStatistics(motoQuery, TransactionType.PAYMENT);
        }

        return new TransactionSummaryResult(payments, motoPayments, refunds, payments.getGrossAmount() - refunds.getGrossAmount());
    }

    public List<TimeseriesReportSlice> getTransactionsByHour(ZonedDateTime fromDate, ZonedDateTime toDate) {
        return reportDao.getTransactionsVolumeByTimeseries(fromDate, toDate);
    }

    private TransactionStatisticQuery buildBaseTransactionStatisticQuery(TransactionSummaryParams params) {

        TransactionStatisticQuery transactionStatisticQuery = new TransactionStatisticQuery();

        if (isNotBlank(params.getAccountId())) {
            transactionStatisticQuery.withAccountId(params.getAccountId());
        }

        if (isNotBlank(params.getFromDate())) {
            transactionStatisticQuery.withFromDate(params.getFromDate());
        }

        if (isNotBlank(params.getToDate())) {
            transactionStatisticQuery.withToDate(params.getToDate());
        }

        return transactionStatisticQuery;
    }
}
