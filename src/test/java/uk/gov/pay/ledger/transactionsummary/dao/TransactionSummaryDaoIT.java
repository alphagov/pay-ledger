package uk.gov.pay.ledger.transactionsummary.dao;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.report.entity.GatewayAccountMonthlyPerformanceReportEntity;
import uk.gov.pay.ledger.report.params.PerformanceReportParams;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.TransactionSummaryFixture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.math.BigDecimal.ZERO;
import static java.time.ZonedDateTime.parse;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.BigDecimalCloseTo.closeTo;
import static uk.gov.pay.ledger.transaction.model.TransactionType.PAYMENT;
import static uk.gov.pay.ledger.transaction.model.TransactionType.REFUND;
import static uk.gov.pay.ledger.transaction.state.TransactionState.FAILED_REJECTED;
import static uk.gov.pay.ledger.transaction.state.TransactionState.STARTED;
import static uk.gov.pay.ledger.transaction.state.TransactionState.SUCCESS;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionSummaryFixture.aTransactionSummaryFixture;

public class TransactionSummaryDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();
    private TransactionSummaryDao transactionSummaryDao;
    private DatabaseTestHelper dbHelper;

    @BeforeEach
    public void setUp() {
        dbHelper = aDatabaseTestHelper(rule.getJdbi());
        dbHelper.truncateTransactionSummaryData();
        transactionSummaryDao = new TransactionSummaryDao(rule.getJdbi());
    }

    @Test
    public void upsertShouldInsertNewTransactionSummaryRowCorrectly() {
        String gatewayAccountId1 = "account-" + randomAlphanumeric(10);
        String gatewayAccountId2 = "account-" + randomAlphanumeric(10);
        transactionSummaryDao.upsert(gatewayAccountId1, "PAYMENT",
                parse("2018-09-22T08:46:01.123456Z"), SUCCESS, false, false, 100L);
        transactionSummaryDao.upsert(gatewayAccountId2, "PAYMENT",
                parse("2018-09-23T09:46:01.123456Z"), FAILED_REJECTED, true, true, 200L);

        List<Map<String, Object>> transactionSummary = dbHelper.getTransactionSummary(gatewayAccountId1, PAYMENT,
                SUCCESS, parse("2018-09-22T00:00:00.000000Z"), false, false);
        assertThat(transactionSummary.get(0).get("gateway_account_id"), is(gatewayAccountId1));
        assertThat(transactionSummary.get(0).get("transaction_date").toString(), is("2018-09-22"));
        assertThat(transactionSummary.get(0).get("state"), is("SUCCESS"));
        assertThat(transactionSummary.get(0).get("live"), is(false));
        assertThat(transactionSummary.get(0).get("moto"), is(false));
        assertThat(transactionSummary.get(0).get("total_amount_in_pence"), is(100L));
        assertThat(transactionSummary.get(0).get("no_of_transactions"), is(1L));

        transactionSummary = dbHelper.getTransactionSummary(gatewayAccountId2, PAYMENT,
                FAILED_REJECTED, parse("2018-09-23T00:00:00.000000Z"), true, true);
        assertThat(transactionSummary.get(0).get("gateway_account_id"), is(gatewayAccountId2));
        assertThat(transactionSummary.get(0).get("transaction_date").toString(), is("2018-09-23"));
        assertThat(transactionSummary.get(0).get("state"), is("FAILED_REJECTED"));
        assertThat(transactionSummary.get(0).get("live"), is(true));
        assertThat(transactionSummary.get(0).get("moto"), is(true));
        assertThat(transactionSummary.get(0).get("total_amount_in_pence"), is(200L));
        assertThat(transactionSummary.get(0).get("no_of_transactions"), is(1L));
    }

    @Test
    public void upsertShouldUpdateExistingTransactionSummaryRowCorrectly() {
        String gatewayAccountId = "account-" + randomAlphanumeric(10);
        TransactionSummaryFixture transactionSummaryFixture = aTransactionSummaryFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionDate(parse("2018-09-22T00:00:00.000000Z"))
                .withType(PAYMENT)
                .withState(SUCCESS)
                .withAmount(1000L)
                .withFee(77L)
                .withNoOfTransactions(10L)
                .insert(rule.getJdbi());
        transactionSummaryDao.upsert(gatewayAccountId, PAYMENT.name(),
                transactionSummaryFixture.getTransactionDate(), transactionSummaryFixture.getState(),
                false, false, 123L);

        List<Map<String, Object>> transactionSummary = dbHelper.getTransactionSummary(gatewayAccountId, PAYMENT,
                SUCCESS, parse("2018-09-22T00:00:00.000000Z"), false, false);
        assertThat(transactionSummary.get(0).get("gateway_account_id"), is(gatewayAccountId));
        assertThat(transactionSummary.get(0).get("transaction_date").toString(), is("2018-09-22"));
        assertThat(transactionSummary.get(0).get("state"), is("SUCCESS"));
        assertThat(transactionSummary.get(0).get("live"), is(false));
        assertThat(transactionSummary.get(0).get("moto"), is(false));
        assertThat(transactionSummary.get(0).get("total_amount_in_pence"), is(1123L));
        assertThat(transactionSummary.get(0).get("no_of_transactions"), is(11L));
    }

    @Test
    public void deductShouldDeductTheSummaryCorrectly() {
        String gatewayAccountId = "account-" + randomAlphanumeric(10);
        TransactionSummaryFixture transactionSummaryFixture = aTransactionSummaryFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionDate(parse("2018-09-22T00:00:00.000000Z"))
                .withType(PAYMENT)
                .withState(SUCCESS)
                .withAmount(1000L)
                .withFee(100L)
                .withNoOfTransactions(10L)
                .insert(rule.getJdbi());
        transactionSummaryDao.deductTransactionSummaryFor(gatewayAccountId, PAYMENT.name(),
                transactionSummaryFixture.getTransactionDate(), transactionSummaryFixture.getState(),
                false, false, 123L, 23L);

        List<Map<String, Object>> transactionSummary = dbHelper.getTransactionSummary(gatewayAccountId, PAYMENT,
                SUCCESS, parse("2018-09-22T00:00:00.000000Z"), false, false);
        assertThat(transactionSummary.get(0).get("gateway_account_id"), is(gatewayAccountId));
        assertThat(transactionSummary.get(0).get("transaction_date").toString(), is("2018-09-22"));
        assertThat(transactionSummary.get(0).get("state"), is("SUCCESS"));
        assertThat(transactionSummary.get(0).get("live"), is(false));
        assertThat(transactionSummary.get(0).get("moto"), is(false));
        assertThat(transactionSummary.get(0).get("total_amount_in_pence"), is(877L));
        assertThat(transactionSummary.get(0).get("no_of_transactions"), is(9L));
        assertThat(transactionSummary.get(0).get("total_fee_in_pence"), is(77L));
    }

    @Test
    public void shouldUpdateFeeForTransactionSummaryCorrectly() {
        String gatewayAccountId = "account-" + randomAlphanumeric(10);
        TransactionSummaryFixture transactionSummaryFixture = aTransactionSummaryFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionDate(parse("2018-09-22T00:00:00.000000Z"))
                .withType(PAYMENT)
                .withState(SUCCESS)
                .withAmount(1000L)
                .withFee(100L)
                .withNoOfTransactions(10L)
                .insert(rule.getJdbi());
        transactionSummaryDao.updateFee(gatewayAccountId, PAYMENT.name(),
                transactionSummaryFixture.getTransactionDate(), transactionSummaryFixture.getState(),
                false, false, 23L);

        List<Map<String, Object>> transactionSummary = dbHelper.getTransactionSummary(gatewayAccountId, PAYMENT,
                SUCCESS, parse("2018-09-22T00:00:00.000000Z"), false, false);
        assertThat(transactionSummary.get(0).get("gateway_account_id"), is(gatewayAccountId));
        assertThat(transactionSummary.get(0).get("transaction_date").toString(), is("2018-09-22"));
        assertThat(transactionSummary.get(0).get("state"), is("SUCCESS"));
        assertThat(transactionSummary.get(0).get("live"), is(false));
        assertThat(transactionSummary.get(0).get("moto"), is(false));
        assertThat(transactionSummary.get(0).get("total_amount_in_pence"), is(1000L));
        assertThat(transactionSummary.get(0).get("total_fee_in_pence"), is(123L));
    }

    @Test
    public void verifyMonthlyGatewayPerformanceReportTest() {
        aTransactionSummaryFixture()
                .withGatewayAccountId("1")
                .withAmount(1000L)
                .withState(STARTED)
                .withType(PAYMENT)
                .withLive(true)
                .withTransactionDate(ZonedDateTime.parse("2019-01-01T02:00:00Z"))
                .insert(rule.getJdbi());

        aTransactionSummaryFixture()
                .withGatewayAccountId("1")
                .withAmount(1000L)
                .withState(SUCCESS)
                .withType(REFUND)
                .withLive(true)
                .withTransactionDate(ZonedDateTime.parse("2019-01-01T02:00:00Z"))
                .insert(rule.getJdbi());

        aTransactionSummaryFixture()
                .withGatewayAccountId("1")
                .withAmount(1000L)
                .withState(SUCCESS)
                .withType(PAYMENT)
                .withLive(true)
                .withTransactionDate(ZonedDateTime.parse("2018-01-01T02:00:00Z"))
                .insert(rule.getJdbi());

        List<String> relevantGatewayAccounts = List.of("1", "2");
        relevantGatewayAccounts.forEach(account -> aTransactionSummaryFixture()
                .withGatewayAccountId(account)
                .withAmount(1000L)
                .withNoOfTransactions(1L)
                .withState(SUCCESS)
                .withType(PAYMENT)
                .withLive(true)
                .withTransactionDate(ZonedDateTime.parse("2019-01-01T02:00:00Z"))
                .insert(rule.getJdbi()));

        BigDecimal expectedValue = BigDecimal.valueOf(1000);
        LocalDate startDate = LocalDate.parse("2019-01-01");
        LocalDate endDate = LocalDate.parse("2019-02-01");

        List<GatewayAccountMonthlyPerformanceReportEntity> performanceReport = transactionSummaryDao.monthlyPerformanceReportForGatewayAccounts(startDate, endDate);

        GatewayAccountMonthlyPerformanceReportEntity gatewayAccountOnePerformanceReport = performanceReport.get(0);
        GatewayAccountMonthlyPerformanceReportEntity gatewayAccountTwoPerformanceReport = performanceReport.get(1);

        assertThat(performanceReport.size(), CoreMatchers.is(2));

        assertThat(gatewayAccountOnePerformanceReport.getGatewayAccountId(), CoreMatchers.is(1L));
        assertThat(gatewayAccountOnePerformanceReport.getTotalVolume(), CoreMatchers.is(1L));
        assertThat(gatewayAccountOnePerformanceReport.getTotalAmount(), CoreMatchers.is(expectedValue));
        assertThat(gatewayAccountOnePerformanceReport.getAverageAmount(), CoreMatchers.is(closeTo(expectedValue, ZERO)));
        assertThat(gatewayAccountOnePerformanceReport.getYear(), CoreMatchers.is(2019L));
        assertThat(gatewayAccountOnePerformanceReport.getMonth(), CoreMatchers.is(1L));

        assertThat(gatewayAccountTwoPerformanceReport.getGatewayAccountId(), CoreMatchers.is(2L));
        assertThat(gatewayAccountTwoPerformanceReport.getTotalVolume(), CoreMatchers.is(1L));
        assertThat(gatewayAccountTwoPerformanceReport.getTotalAmount(), CoreMatchers.is(expectedValue));
        assertThat(gatewayAccountTwoPerformanceReport.getAverageAmount(), CoreMatchers.is(closeTo(expectedValue, ZERO)));
        assertThat(gatewayAccountTwoPerformanceReport.getYear(), CoreMatchers.is(2019L));
        assertThat(gatewayAccountTwoPerformanceReport.getMonth(), CoreMatchers.is(1L));
    }

    @Test
    public void report_volume_total_amount_and_average_amount_for_date_range() {
        Stream.of("2019-12-31T10:00:00Z", "2019-12-30T10:00:00Z", "2017-11-29T10:00:00Z").forEach(time -> aTransactionSummaryFixture()
                .withTransactionDate(ZonedDateTime.parse(time))
                .withAmount(100L)
                .withState(TransactionState.SUCCESS)
                .withType(PAYMENT)
                .withLive(true)
                .withNoOfTransactions(1L)
                .insert(rule.getJdbi()));

        Stream.of("2019-12-12T10:00:00Z", "2019-12-11T10:00:00Z", "2017-11-30T10:00:00Z").forEach(time -> aTransactionSummaryFixture()
                .withTransactionDate(ZonedDateTime.parse(time))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withType(PAYMENT)
                .withLive(true)
                .withNoOfTransactions(1L)
                .insert(rule.getJdbi()));

        var performanceReportParams = PerformanceReportParams.PerformanceReportParamsBuilder.builder()
                .withFromDate(LocalDate.parse("2017-11-30"))
                .withToDate(LocalDate.parse("2019-12-12"))
                .build();
        var performanceReportEntity = transactionSummaryDao.performanceReportForPaymentTransactions(performanceReportParams);
        assertThat(performanceReportEntity.getTotalVolume(), CoreMatchers.is(3L));
        assertThat(performanceReportEntity.getTotalAmount(), CoreMatchers.is(closeTo(new BigDecimal(3000L), ZERO)));
        assertThat(performanceReportEntity.getAverageAmount(), CoreMatchers.is(closeTo(new BigDecimal(1000L), ZERO)));
    }

    @Test
    public void report_volume_total_amount_and_average_amount_by_state() {
        aTransactionSummaryFixture().withAmount(1000L).withState(TransactionState.STARTED).withType(PAYMENT)
                .withLive(true).withNoOfTransactions(1L).insert(rule.getJdbi());

        aTransactionSummaryFixture().withAmount(1000L).withState(TransactionState.SUCCESS).withType(REFUND)
                .withLive(true).withNoOfTransactions(1L).insert(rule.getJdbi());

        aTransactionSummaryFixture().withAmount(1000L).withState(TransactionState.SUCCESS).withType(PAYMENT)
                .withLive(false).withNoOfTransactions(1L).insert(rule.getJdbi());

        List<Long> relevantAmounts = List.of(1200L, 1020L, 750L);
        relevantAmounts.forEach(amount -> aTransactionSummaryFixture()
                .withAmount(amount)
                .withState(TransactionState.SUCCESS)
                .withType(PAYMENT)
                .withLive(true)
                .withNoOfTransactions(1L)
                .insert(rule.getJdbi()));
        BigDecimal expectedTotalAmount = new BigDecimal(relevantAmounts.stream().mapToLong(amount -> amount).sum());
        BigDecimal expectedAverageAmount = BigDecimal.valueOf(relevantAmounts.stream().mapToLong(amount -> amount).average().getAsDouble());

        var performanceReportParams = PerformanceReportParams.PerformanceReportParamsBuilder.builder().withState(TransactionState.SUCCESS).build();
        var performanceReportEntity = transactionSummaryDao.performanceReportForPaymentTransactions(performanceReportParams);
        assertThat(performanceReportEntity.getTotalVolume(), CoreMatchers.is(3L));
        assertThat(performanceReportEntity.getTotalAmount(), CoreMatchers.is(closeTo(expectedTotalAmount, ZERO)));
        assertThat(performanceReportEntity.getAverageAmount(), CoreMatchers.is(closeTo(expectedAverageAmount, ZERO)));
    }

    @Test
    public void report_volume_total_amount_and_average_amount_for_all_payments() {
        Stream.of(TransactionState.STARTED, TransactionState.SUCCESS, TransactionState.SUBMITTED).forEach(state ->
                aTransactionSummaryFixture()
                        .withAmount(1000L)
                        .withState(state)
                        .withType(PAYMENT)
                        .withLive(true)
                        .withNoOfTransactions(1L)
                        .insert(rule.getJdbi()));

        var performanceReportParams = PerformanceReportParams.PerformanceReportParamsBuilder.builder().build();
        var performanceReportEntity = transactionSummaryDao.performanceReportForPaymentTransactions(performanceReportParams);
        assertThat(performanceReportEntity.getTotalVolume(), CoreMatchers.is(3L));
        assertThat(performanceReportEntity.getTotalAmount(), CoreMatchers.is(closeTo(new BigDecimal(3000L), ZERO)));
        assertThat(performanceReportEntity.getAverageAmount(), CoreMatchers.is(closeTo(new BigDecimal(1000L), ZERO)));
    }
}