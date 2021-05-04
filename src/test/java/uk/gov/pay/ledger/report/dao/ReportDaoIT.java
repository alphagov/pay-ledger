package uk.gov.pay.ledger.report.dao;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.report.dao.builder.TransactionStatisticQuery;
import uk.gov.pay.ledger.report.entity.PaymentCountByStateResult;
import uk.gov.pay.ledger.report.entity.TimeseriesReportSlice;
import uk.gov.pay.ledger.report.entity.TransactionsStatisticsResult;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class ReportDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private ReportDao reportDao;

    private DatabaseTestHelper databaseTestHelper = aDatabaseTestHelper(rule.getJdbi());

    @BeforeEach
    public void setUp() {
        databaseTestHelper.truncateAllData();
        reportDao = new ReportDao(rule.getJdbi());
    }

    @Test
    public void shouldReturnCountsForStatuses_whenSearchingWithNoParameters() {
        aTransactionFixture().withState(TransactionState.CREATED).insert(rule.getJdbi());
        aTransactionFixture().withState(TransactionState.CREATED).insert(rule.getJdbi());
        aTransactionFixture().withState(TransactionState.ERROR).insert(rule.getJdbi());
        aTransactionFixture().withState(TransactionState.SUCCESS).insert(rule.getJdbi());

        var query = new TransactionStatisticQuery();

        List<PaymentCountByStateResult> paymentCountsByState = reportDao.getPaymentCountsByState(query);

        assertThat(paymentCountsByState, hasSize(3));
        assertThat(paymentCountsByState, containsInAnyOrder(
                is(new PaymentCountByStateResult("CREATED", 2L)),
                is(new PaymentCountByStateResult("ERROR", 1L)),
                is(new PaymentCountByStateResult("SUCCESS", 1L))
        ));
    }

    @Test
    public void shouldReturnCountsForStatuses_whenSearchingByGatewayAccount() {
        String gatewayAccountId1 = "account-1";
        String gatewayAccountId2 = "account-2";

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withState(TransactionState.CREATED)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId2)
                .withState(TransactionState.CREATED)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withState(TransactionState.ERROR)
                .insert(rule.getJdbi());

        var query = new TransactionStatisticQuery()
                .withAccountId(gatewayAccountId1);

        List<PaymentCountByStateResult> paymentCountsByState = reportDao.getPaymentCountsByState(query);

        assertThat(paymentCountsByState, hasSize(2));
        assertThat(paymentCountsByState, containsInAnyOrder(
                is(new PaymentCountByStateResult("CREATED", 1L)),
                is(new PaymentCountByStateResult("ERROR", 1L))
        ));
    }

    @Test
    public void shouldReturnCountsForStatuses_whenSearchingByFromDateAndToDate() {
        aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withCreatedDate(ZonedDateTime.parse("2019-09-29T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withState(TransactionState.ERROR)
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withState(TransactionState.SUCCESS)
                .withCreatedDate(ZonedDateTime.parse("2019-09-29T00:00:00.000Z"))
                .insert(rule.getJdbi());

        var query = new TransactionStatisticQuery()
                .withFromDate("2019-09-29T23:59:59.000Z")
                .withToDate("2019-10-01T00:00:00.000Z");

        List<PaymentCountByStateResult> paymentCountsByState = reportDao.getPaymentCountsByState(query);

        assertThat(paymentCountsByState, hasSize(2));
        assertThat(paymentCountsByState, containsInAnyOrder(
                is(new PaymentCountByStateResult("CREATED", 1L)),
                is(new PaymentCountByStateResult("ERROR", 1L))
        ));
    }

    @Test
    public void shouldReturnCountsForStatuses_whenSearchingByAllParameters() {
        String gatewayAccountId1 = "account-1";
        String gatewayAccountId2 = "account-2";

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withState(TransactionState.CREATED)
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withState(TransactionState.CREATED)
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withState(TransactionState.CREATED)
                .withCreatedDate(ZonedDateTime.parse("2019-09-29T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId2)
                .withState(TransactionState.CREATED)
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withState(TransactionState.ERROR)
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .insert(rule.getJdbi());

        var query = new TransactionStatisticQuery()
                .withAccountId(gatewayAccountId1)
                .withFromDate("2019-09-29T23:59:59.000Z")
                .withToDate("2019-10-01T00:00:00.000Z");

        List<PaymentCountByStateResult> paymentCountsByState = reportDao.getPaymentCountsByState(query);

        assertThat(paymentCountsByState, hasSize(2));
        assertThat(paymentCountsByState, containsInAnyOrder(
                is(new PaymentCountByStateResult("CREATED", 1L)),
                is(new PaymentCountByStateResult("ERROR", 1L))
        ));
    }

    @Test
    public void shouldReturnPaymentsStatisticsForSuccessfulPayments_whenSearchingWithNoParameters() {
        aTransactionFixture()
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withAmount(2000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withAmount(3000L)
                .withState(TransactionState.CREATED)
                .insert(rule.getJdbi());

        var query = new TransactionStatisticQuery();

        TransactionsStatisticsResult paymentsStatistics = reportDao.getTransactionSummaryStatistics(query, TransactionType.PAYMENT);

        assertThat(paymentsStatistics.getCount(), is(2L));
        assertThat(paymentsStatistics.getGrossAmount(), is(3000L));
    }

    @Test
    public void shouldReturnPaymentsStatistics_whenSearchingByGatewayAccount() {
        String gatewayAccountId1 = "account-1";
        String gatewayAccountId2 = "account-2";

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId1)
                .withAmount(2000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId2)
                .withAmount(3000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());

        var query = new TransactionStatisticQuery()
                .withAccountId(gatewayAccountId1);

        TransactionsStatisticsResult paymentsStatistics = reportDao.getTransactionSummaryStatistics(query, TransactionType.PAYMENT);

        assertThat(paymentsStatistics.getCount(), is(2L));
        assertThat(paymentsStatistics.getGrossAmount(), is(3000L));
    }

    @Test
    public void shouldReturnPaymentsStatistics_whenSearchingByFromDateAndToDate() {

        aTransactionFixture()
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T00:00:00.000Z"))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withAmount(2000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withAmount(3000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withCreatedDate(ZonedDateTime.parse("2019-09-29T00:00:00.000Z"))
                .withAmount(4000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());

        var query = new TransactionStatisticQuery()
                .withFromDate("2019-09-29T23:59:59.000Z")
                .withToDate("2019-10-01T00:00:00.000Z");

        TransactionsStatisticsResult paymentsStatistics = reportDao.getTransactionSummaryStatistics(query, TransactionType.PAYMENT);

        assertThat(paymentsStatistics.getCount(), is(2L));
        assertThat(paymentsStatistics.getGrossAmount(), is(5000L));
    }

    @Test
    public void shouldReturnTransactionsSummary_whenSearchingByFromDate() {
        String gatewayAccountId = RandomStringUtils.randomAlphanumeric(10);
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withAmount(2000L)
                .withState(TransactionState.FAILED_REJECTED)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withAmount(4000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-29T00:00:00.000Z"))
                .withAmount(4000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId("another-gateway-account")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-29T00:00:00.000Z"))
                .withAmount(4000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.REFUND.name())
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T00:00:00.000Z"))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId("another-gateway-account")
                .withTransactionType(TransactionType.REFUND.name())
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T00:00:00.000Z"))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.REFUND.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-28T00:00:00.000Z"))
                .withAmount(3000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.REFUND.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withAmount(2000L)
                .withState(TransactionState.FAILED_REJECTED)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withAmount(1000L)
                .withMoto(true)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());

        var queryWithoutMoto = new TransactionStatisticQuery()
                .withAccountId(gatewayAccountId)
                .withFromDate("2019-09-29T23:59:59.000Z");

        var queryWithMoto = new TransactionStatisticQuery()
                .withAccountId(gatewayAccountId)
                .withMoto(true)
                .withFromDate("2019-09-29T23:59:59.000Z");

        TransactionsStatisticsResult paymentsStatistics = reportDao.getTransactionSummaryStatistics(queryWithoutMoto, TransactionType.PAYMENT);
        assertThat(paymentsStatistics.getCount(), is(2L));
        assertThat(paymentsStatistics.getGrossAmount(), is(5000L));

        TransactionsStatisticsResult refundsStatistics = reportDao.getTransactionSummaryStatistics(queryWithoutMoto, TransactionType.REFUND);
        assertThat(refundsStatistics.getCount(), is(1L));
        assertThat(refundsStatistics.getGrossAmount(), is(1000L));

        TransactionsStatisticsResult motoPaymentsStatistics = reportDao.getTransactionSummaryStatistics(queryWithMoto, TransactionType.PAYMENT);
        assertThat(motoPaymentsStatistics.getCount(), is(1L));
        assertThat(motoPaymentsStatistics.getGrossAmount(), is(1000L));
    }

    @Test
    public void shouldSumAmountWhenTotalAmountIsNull() {
        String gatewayAccountId = RandomStringUtils.randomAlphanumeric(10);
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withAmount(1000L)
                .withTotalAmount(1200L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withAmount(1000L)
                .withTotalAmount(1200L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.REFUND.name())
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T00:00:00.000Z"))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());

        var query = new TransactionStatisticQuery()
                .withAccountId(gatewayAccountId)
                .withFromDate("2019-09-29T23:59:59.000Z");

        TransactionsStatisticsResult paymentsStatistics = reportDao.getTransactionSummaryStatistics(query, TransactionType.PAYMENT);
        assertThat(paymentsStatistics.getCount(), is(4L));
        assertThat(paymentsStatistics.getGrossAmount(), is(4400L));

        TransactionsStatisticsResult refundsStatistics = reportDao.getTransactionSummaryStatistics(query, TransactionType.REFUND);
        assertThat(refundsStatistics.getCount(), is(1L));
        assertThat(refundsStatistics.getGrossAmount(), is(1000L));
    }

    @Test
    public void shouldGetTransactionVolumesGroupedByTimeseries() {
        aTransactionFixture()
                .withGatewayAccountId("100")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.100Z"))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withLive(true)
                .insert(rule.getJdbi())
                .toEntity();

        aTransactionFixture()
                .withGatewayAccountId("100")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T08:30:00.000Z"))
                .withAmount(1000L)
                .withTotalAmount(1500L)
                .withState(TransactionState.SUCCESS)
                .withLive(true)
                .insert(rule.getJdbi())
                .toEntity();

        aTransactionFixture()
                .withGatewayAccountId("100")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T08:40:00.000Z"))
                .withAmount(1000L)
                .withTotalAmount(1500L)
                .withState(TransactionState.SUBMITTED)
                .withLive(true)
                .insert(rule.getJdbi())
                .toEntity();

        aTransactionFixture()
                .withGatewayAccountId("100")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T09:20:00.000Z"))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withLive(true)
                .insert(rule.getJdbi())
                .toEntity();

        aTransactionFixture()
                .withGatewayAccountId("100")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T18:20:00.000Z"))
                .withAmount(1000L)
                .withState(TransactionState.ERROR)
                .withLive(true)
                .insert(rule.getJdbi())
                .toEntity();

        List<TimeseriesReportSlice> timeseriesReportSlices = reportDao.getTransactionsVolumeByTimeseries(
                ZonedDateTime.parse("2019-09-30T00:00:00.000Z"),
                ZonedDateTime.parse("2019-09-30T23:59:59.999Z")
        );
        assertThat(timeseriesReportSlices.size(), is(4));

        assertThat(timeseriesReportSlices.get(0).getTimestamp().getHour(), is(0));
        assertThat(timeseriesReportSlices.get(0).getAllPayments(), is(1));
        assertThat(timeseriesReportSlices.get(0).getCompletedPayments(), is(1));

        assertThat(timeseriesReportSlices.get(1).getTimestamp().getHour(), is(8));
        assertThat(timeseriesReportSlices.get(1).getAmount(), is(2000));
        assertThat(timeseriesReportSlices.get(1).getTotalAmount(), is(3000));
        assertThat(timeseriesReportSlices.get(1).getAllPayments(), is(2));
        assertThat(timeseriesReportSlices.get(1).getCompletedPayments(), is(1));
        assertThat(timeseriesReportSlices.get(1).getErroredPayments(), is(0));

        assertThat(timeseriesReportSlices.get(2).getTimestamp().getHour(), is(9));
        assertThat(timeseriesReportSlices.get(2).getAllPayments(), is(1));
        assertThat(timeseriesReportSlices.get(2).getCompletedPayments(), is(1));
        assertThat(timeseriesReportSlices.get(2).getErroredPayments(), is(0));
        assertThat(timeseriesReportSlices.get(2).getAmount(), is(1000));

        assertThat(timeseriesReportSlices.get(3).getTimestamp().getHour(), is(18));
        assertThat(timeseriesReportSlices.get(3).getAllPayments(), is(1));
        assertThat(timeseriesReportSlices.get(3).getCompletedPayments(), is(0));
        assertThat(timeseriesReportSlices.get(3).getErroredPayments(), is(1));
    }
}