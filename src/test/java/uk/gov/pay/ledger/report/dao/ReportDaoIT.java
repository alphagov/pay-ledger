package uk.gov.pay.ledger.report.dao;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.report.entity.PaymentCountByStateResult;
import uk.gov.pay.ledger.report.entity.TransactionsStatisticsResult;
import uk.gov.pay.ledger.report.params.TransactionSummaryParams;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
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
    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private ReportDao reportDao;

    private DatabaseTestHelper databaseTestHelper = aDatabaseTestHelper(rule.getJdbi());

    @Before
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

        var params = new TransactionSummaryParams();

        List<PaymentCountByStateResult> paymentCountsByState = reportDao.getPaymentCountsByState(params);

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

        var params = new TransactionSummaryParams();
        params.setAccountId(gatewayAccountId1);

        List<PaymentCountByStateResult> paymentCountsByState = reportDao.getPaymentCountsByState(params);

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

        var params = new TransactionSummaryParams();
        params.setFromDate("2019-09-29T23:59:59.000Z");
        params.setToDate("2019-10-01T00:00:00.000Z");

        List<PaymentCountByStateResult> paymentCountsByState = reportDao.getPaymentCountsByState(params);

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

        var params = new TransactionSummaryParams();
        params.setAccountId(gatewayAccountId1);
        params.setFromDate("2019-09-29T23:59:59.000Z");
        params.setToDate("2019-10-01T00:00:00.000Z");

        List<PaymentCountByStateResult> paymentCountsByState = reportDao.getPaymentCountsByState(params);

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

        var params = new TransactionSummaryParams();

        TransactionsStatisticsResult paymentsStatistics = reportDao.getTransactionSummaryStatistics(params, TransactionType.PAYMENT);

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

        var params = new TransactionSummaryParams();
        params.setAccountId(gatewayAccountId1);

        TransactionsStatisticsResult paymentsStatistics = reportDao.getTransactionSummaryStatistics(params, TransactionType.PAYMENT);

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

        var params = new TransactionSummaryParams();
        params.setFromDate("2019-09-29T23:59:59.000Z");
        params.setToDate("2019-10-01T00:00:00.000Z");

        TransactionsStatisticsResult paymentsStatistics = reportDao.getTransactionSummaryStatistics(params, TransactionType.PAYMENT);

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

        var transactionParams = new TransactionSummaryParams();
        transactionParams.setAccountId(gatewayAccountId);
        transactionParams.setFromDate("2019-09-29T23:59:59.000Z");

        TransactionsStatisticsResult paymentsStatistics = reportDao.getTransactionSummaryStatistics(transactionParams, TransactionType.PAYMENT);
        assertThat(paymentsStatistics.getCount(), is(1L));
        assertThat(paymentsStatistics.getGrossAmount(), is(4000L));

        TransactionsStatisticsResult refundsStatistics = reportDao.getTransactionSummaryStatistics(transactionParams, TransactionType.REFUND);
        assertThat(refundsStatistics.getCount(), is(1L));
        assertThat(refundsStatistics.getGrossAmount(), is(1000L));
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
        var transactionParams = new TransactionSummaryParams();
        transactionParams.setAccountId(gatewayAccountId);
        transactionParams.setFromDate("2019-09-29T23:59:59.000Z");

        TransactionsStatisticsResult paymentsStatistics = reportDao.getTransactionSummaryStatistics(transactionParams, TransactionType.PAYMENT);
        assertThat(paymentsStatistics.getCount(), is(4L));
        assertThat(paymentsStatistics.getGrossAmount(), is(4400L));

        TransactionsStatisticsResult refundsStatistics = reportDao.getTransactionSummaryStatistics(transactionParams, TransactionType.REFUND);
        assertThat(refundsStatistics.getCount(), is(1L));
        assertThat(refundsStatistics.getGrossAmount(), is(1000L));
    }
}