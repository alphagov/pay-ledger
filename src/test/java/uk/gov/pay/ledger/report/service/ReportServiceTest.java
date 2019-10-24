package uk.gov.pay.ledger.report.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.report.dao.ReportDao;
import uk.gov.pay.ledger.report.entity.PaymentCountByStateResult;
import uk.gov.pay.ledger.report.entity.PaymentsStatisticsResult;
import uk.gov.pay.ledger.report.entity.TransactionSummaryResult;
import uk.gov.pay.ledger.report.params.PaymentsReportParams;
import uk.gov.pay.ledger.report.params.TransactionSummaryParams;
import uk.gov.pay.ledger.transaction.model.TransactionType;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.transaction.state.TransactionState.CREATED;
import static uk.gov.pay.ledger.transaction.state.TransactionState.ERROR;
import static uk.gov.pay.ledger.transaction.state.TransactionState.SUCCESS;

@RunWith(MockitoJUnitRunner.class)
public class ReportServiceTest {

    @Mock
    private ReportDao mockReportDao;

    @Captor
    private ArgumentCaptor<PaymentsReportParams> paymentsReportParamsCaptor;

    @InjectMocks
    private ReportService reportService;

    @Test
    public void shouldReturnPaymentCountsByState_whenGatewayAccountIdProvided() {
        String gatewayAccountId = "1";
        String fromDate = "2019-10-1T10:00:00.000Z";
        String toDate = "2019-10-1T11:00:00.000Z";
        var params = new PaymentsReportParams();
        params.setFromDate(fromDate);
        params.setToDate(toDate);

        when(mockReportDao.getPaymentCountsByState(paymentsReportParamsCaptor.capture())).thenReturn(List.of(
                new PaymentCountByStateResult(CREATED.name(), 2L),
                new PaymentCountByStateResult(SUCCESS.name(), 1L),
                new PaymentCountByStateResult(ERROR.name(), 3L)
        ));

        Map<String, Long> response = reportService.getPaymentCountsByState(gatewayAccountId, params);

        assertThat(paymentsReportParamsCaptor.getValue().getAccountId(), is(gatewayAccountId));
        assertThat(paymentsReportParamsCaptor.getValue().getFromDate(), is(fromDate));
        assertThat(paymentsReportParamsCaptor.getValue().getToDate(), is(toDate));

        assertThat(response, allOf(
                aMapWithSize(10),
                hasEntry("undefined", 0L),
                hasEntry("created", 2L),
                hasEntry("started", 0L),
                hasEntry("submitted", 0L),
                hasEntry("capturable", 0L),
                hasEntry("success", 1L),
                hasEntry("declined", 0L),
                hasEntry("timedout", 0L),
                hasEntry("cancelled", 0L),
                hasEntry("error", 3L)
        ));
    }

    @Test
    public void shouldReturnPaymentCountsByState_whenGatewayAccountIdNotProvided() {
        var params = new PaymentsReportParams();
        params.setFromDate("2019-10-1T10:00:00.000Z");
        params.setToDate("2019-10-1T11:00:00.000Z");

        when(mockReportDao.getPaymentCountsByState(paymentsReportParamsCaptor.capture())).thenReturn(List.of(
                new PaymentCountByStateResult(CREATED.name(), 2L),
                new PaymentCountByStateResult(SUCCESS.name(), 1L)
        ));

        Map<String, Long> response = reportService.getPaymentCountsByState(null, params);

        assertThat(paymentsReportParamsCaptor.getValue().getAccountId(), is(nullValue()));
        assertThat(response, aMapWithSize(10));
    }

    @Test
    public void shouldReturnPaymentsStatistics_whenGatewayAccountIdProvided() {
        String gatewayAccountId = "1";
        String fromDate = "2019-10-1T10:00:00.000Z";
        String toDate = "2019-10-1T11:00:00.000Z";
        var params = new PaymentsReportParams();
        params.setFromDate(fromDate);
        params.setToDate(toDate);

        PaymentsStatisticsResult result = new PaymentsStatisticsResult(5L, 10000L);
        when(mockReportDao.getPaymentsStatistics(paymentsReportParamsCaptor.capture(), eq(TransactionType.PAYMENT)))
                .thenReturn(result);

        PaymentsStatisticsResult paymentsStatistics = reportService.getPaymentsStatistics(gatewayAccountId, params);

        assertThat(paymentsReportParamsCaptor.getValue().getAccountId(), is(gatewayAccountId));
        assertThat(paymentsReportParamsCaptor.getValue().getFromDate(), is(fromDate));
        assertThat(paymentsReportParamsCaptor.getValue().getToDate(), is(toDate));

        assertThat(paymentsStatistics, is(result));
    }

    @Test
    public void shouldReturnPaymentsStatistics_whenGatewayAccountIdNotProvided() {
        var params = new PaymentsReportParams();

        PaymentsStatisticsResult result = new PaymentsStatisticsResult(5L, 10000L);
        when(mockReportDao.getPaymentsStatistics(paymentsReportParamsCaptor.capture(), eq(TransactionType.PAYMENT)))
                .thenReturn(result);

        PaymentsStatisticsResult paymentsStatistics = reportService.getPaymentsStatistics(null, params);

        assertThat(paymentsReportParamsCaptor.getValue().getAccountId(), is(nullValue()));
        assertThat(paymentsStatistics, is(result));
    }

    @Test
    public void shouldReturnTransactionsSummary() {
        String gatewayAccountId = "1";
        String fromDate = "2019-10-1T10:00:00.000Z";
        TransactionSummaryParams params = new TransactionSummaryParams();
        params.setAccountId(gatewayAccountId);
        params.setFromDate(fromDate);
        PaymentsStatisticsResult payments = new PaymentsStatisticsResult(5L, 10000L);
        PaymentsStatisticsResult refunds = new PaymentsStatisticsResult(1L, 1000L);
        TransactionSummaryResult result = new TransactionSummaryResult(payments, refunds);
        when(mockReportDao.getPaymentsStatistics(params, TransactionType.PAYMENT)).thenReturn(payments);
        when(mockReportDao.getPaymentsStatistics(params, TransactionType.REFUND)).thenReturn(refunds);
        TransactionSummaryResult dashboardStatisticsResult = reportService.getTransactionsSummary(params);
        assertThat(dashboardStatisticsResult, is(result));
    }
}