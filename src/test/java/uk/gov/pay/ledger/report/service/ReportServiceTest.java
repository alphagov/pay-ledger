package uk.gov.pay.ledger.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.report.dao.ReportDao;
import uk.gov.pay.ledger.report.dao.builder.TransactionStatisticQuery;
import uk.gov.pay.ledger.report.entity.PaymentCountByStateResult;
import uk.gov.pay.ledger.report.entity.TransactionSummaryResult;
import uk.gov.pay.ledger.report.entity.TransactionsStatisticsResult;
import uk.gov.pay.ledger.report.params.TransactionSummaryParams;
import uk.gov.pay.ledger.transaction.model.TransactionType;

import java.time.ZonedDateTime;
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

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private ReportDao mockReportDao;

    @Captor
    private ArgumentCaptor<TransactionStatisticQuery> transactionStatisticQueryArgumentCaptor;

    @InjectMocks
    private ReportService reportService;

    @Test
    public void shouldReturnPaymentCountsByState_whenGatewayAccountIdProvided() {
        String gatewayAccountId = "1";
        String fromDate = "2019-10-01T10:00:00.000Z";
        String toDate = "2019-10-01T11:00:00.000Z";

        var params = new TransactionSummaryParams();
        params.setFromDate(fromDate);
        params.setToDate(toDate);
        params.setAccountId(gatewayAccountId);

        when(mockReportDao.getPaymentCountsByState(transactionStatisticQueryArgumentCaptor.capture())).thenReturn(List.of(
                new PaymentCountByStateResult(CREATED.name(), 2L),
                new PaymentCountByStateResult(SUCCESS.name(), 1L),
                new PaymentCountByStateResult(ERROR.name(), 3L)
        ));

        Map<String, Long> response = reportService.getPaymentCountsByState(params);

        assertThat(transactionStatisticQueryArgumentCaptor.getValue().getQueryMap().get("account_id"), is(gatewayAccountId));
        assertThat(transactionStatisticQueryArgumentCaptor.getValue().getQueryMap().get("from_date"), is(ZonedDateTime.parse(fromDate)));
        assertThat(transactionStatisticQueryArgumentCaptor.getValue().getQueryMap().get("to_date"), is(ZonedDateTime.parse(toDate)));

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
        var params = new TransactionSummaryParams();
        params.setFromDate("2019-10-01T10:00:00.000Z");
        params.setToDate("2019-10-01T11:00:00.000Z");

        when(mockReportDao.getPaymentCountsByState(transactionStatisticQueryArgumentCaptor.capture())).thenReturn(List.of(
                new PaymentCountByStateResult(CREATED.name(), 2L),
                new PaymentCountByStateResult(SUCCESS.name(), 1L)
        ));

        Map<String, Long> response = reportService.getPaymentCountsByState(params);

        assertThat(transactionStatisticQueryArgumentCaptor.getValue().getQueryMap().get("account_id"), is(nullValue()));
        assertThat(response, aMapWithSize(10));
    }

    @Test
    public void shouldReturnTransactionSummaryStatistics_whenMotoProvided() {
        String gatewayAccountId = "1";
        String fromDate = "2019-10-01T10:00:00.000Z";
        String toDate = "2019-10-01T11:00:00.000Z";
        var params = new TransactionSummaryParams();
        params.setFromDate(fromDate);
        params.setToDate(toDate);
        params.setAccountId(gatewayAccountId);
        params.setIncludeMotoStatistics(true);

        var result = new TransactionSummaryResult(
                new TransactionsStatisticsResult(10L, 10000L),
                new TransactionsStatisticsResult(5L, 2000L),
                new TransactionsStatisticsResult(0L, 0L),
                10000L
        );

        var transactionStatisticQueryWithoutMoto = new TransactionStatisticQuery()
                .withAccountId(params.getAccountId())
                .withFromDate(params.getFromDate())
                .withToDate(params.getToDate());

        var transactionStatisticQueryWithMoto = new TransactionStatisticQuery()
                .withAccountId(params.getAccountId())
                .withFromDate(params.getFromDate())
                .withToDate(params.getToDate())
                .withMoto(params.isIncludeMotoStatistics());

        when(mockReportDao.getTransactionSummaryStatistics(eq(transactionStatisticQueryWithoutMoto), eq(TransactionType.PAYMENT)))
                .thenReturn(result.getPayments());
        when(mockReportDao.getTransactionSummaryStatistics(eq(transactionStatisticQueryWithoutMoto), eq(TransactionType.REFUND)))
                .thenReturn(result.getRefunds());
        when(mockReportDao.getTransactionSummaryStatistics(eq(transactionStatisticQueryWithMoto), eq(TransactionType.PAYMENT)))
                .thenReturn(result.getMotoPayments());

        var transactionStatistics = reportService.getTransactionsSummary(params);

        assertThat(transactionStatistics, is(result));
    }

    @Test
    public void shouldReturnTransactionSummaryStatistics_whenGatewayAccountIdProvided() {
        String gatewayAccountId = "1";
        String fromDate = "2019-10-01T10:00:00.000Z";
        String toDate = "2019-10-01T11:00:00.000Z";
        var params = new TransactionSummaryParams();
        params.setFromDate(fromDate);
        params.setToDate(toDate);
        params.setAccountId(gatewayAccountId);

        var result = new TransactionSummaryResult(new TransactionsStatisticsResult(5L, 10000L), new TransactionsStatisticsResult(0L, 0L), new TransactionsStatisticsResult(0L, 0L), 10000L);
        when(mockReportDao.getTransactionSummaryStatistics(transactionStatisticQueryArgumentCaptor.capture(), eq(TransactionType.PAYMENT)))
                .thenReturn(result.getPayments());
        when(mockReportDao.getTransactionSummaryStatistics(transactionStatisticQueryArgumentCaptor.capture(), eq(TransactionType.REFUND)))
                .thenReturn(result.getRefunds());

        var transactionStatistics = reportService.getTransactionsSummary(params);

        assertThat(transactionStatisticQueryArgumentCaptor.getValue().getQueryMap().get("account_id"), is(gatewayAccountId));
        assertThat(transactionStatisticQueryArgumentCaptor.getValue().getQueryMap().get("from_date"), is(ZonedDateTime.parse(fromDate)));
        assertThat(transactionStatisticQueryArgumentCaptor.getValue().getQueryMap().get("to_date"), is(ZonedDateTime.parse(toDate)));

        assertThat(transactionStatistics, is(result));
    }
}