package uk.gov.pay.ledger.report.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.report.dao.ReportDao;
import uk.gov.pay.ledger.report.entity.TransactionSummaryResult;
import uk.gov.pay.ledger.report.entity.PaymentsStatisticsResult;
import uk.gov.pay.ledger.report.params.PaymentsReportParams;
import uk.gov.pay.ledger.report.params.TransactionSummaryParams;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ReportService {

    private final ReportDao reportDao;

    @Inject
    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public Map<String, Long> getPaymentCountsByState(String gatewayAccountId, PaymentsReportParams params) {
        if (isNotBlank(gatewayAccountId)) {
            params.setAccountId(gatewayAccountId);
        }

        // return map with all states, with count of 0 if no payments exist for state
        Map<String, Long> responseMap = new HashMap<>();
        Arrays.stream(TransactionState.values()).forEach(state -> responseMap.put(state.getStatus() , 0L));

        reportDao.getPaymentCountsByState(params).forEach(result ->
                responseMap.put(TransactionState.from(result.getState()).getStatus(), result.getCount()));

        return responseMap;
    }

    public PaymentsStatisticsResult getPaymentsStatistics(String gatewayAccountId, PaymentsReportParams params) {
        if (isNotBlank(gatewayAccountId)) {
            params.setAccountId(gatewayAccountId);
        }

        return reportDao.getPaymentsStatistics(params, TransactionType.PAYMENT);
    }

    public TransactionSummaryResult getTransactionsSummary(TransactionSummaryParams params) {
        PaymentsStatisticsResult payments = reportDao.getPaymentsStatistics(params, TransactionType.PAYMENT);
        PaymentsStatisticsResult refunds = reportDao.getPaymentsStatistics(params, TransactionType.REFUND);
        return new TransactionSummaryResult(payments, refunds);
    }
}
