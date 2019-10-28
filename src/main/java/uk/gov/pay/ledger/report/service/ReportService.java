package uk.gov.pay.ledger.report.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.report.dao.ReportDao;
import uk.gov.pay.ledger.report.entity.TransactionsStatisticsResult;
import uk.gov.pay.ledger.report.entity.TransactionSummaryResult;
import uk.gov.pay.ledger.report.params.TransactionSummaryParams;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ReportService {

    private final ReportDao reportDao;

    @Inject
    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public Map<String, Long> getPaymentCountsByState(TransactionSummaryParams params) {
        // return map with all states, with count of 0 if no payments exist for state
        Map<String, Long> responseMap = new HashMap<>();
        Arrays.stream(TransactionState.values()).forEach(state -> responseMap.put(state.getStatus() , 0L));

        reportDao.getPaymentCountsByState(params).forEach(result ->
                responseMap.put(TransactionState.from(result.getState()).getStatus(), result.getCount()));

        return responseMap;
    }

    public TransactionSummaryResult getTransactionsSummary(TransactionSummaryParams params) {
        TransactionsStatisticsResult payments = reportDao.getTransactionSummaryStatistics(params, TransactionType.PAYMENT);
        TransactionsStatisticsResult refunds = reportDao.getTransactionSummaryStatistics(params, TransactionType.REFUND);
        return new TransactionSummaryResult(payments, refunds);
    }
}
