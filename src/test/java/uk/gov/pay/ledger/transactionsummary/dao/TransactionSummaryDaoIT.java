package uk.gov.pay.ledger.transactionsummary.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.TransactionSummaryFixture;

import java.util.List;
import java.util.Map;

import static java.time.ZonedDateTime.parse;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.ledger.transaction.model.TransactionType.PAYMENT;
import static uk.gov.pay.ledger.transaction.state.TransactionState.FAILED_REJECTED;
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
                parse("2018-09-22T08:46:01.123456Z"), SUCCESS, false, false, 100L, 10L);
        transactionSummaryDao.upsert(gatewayAccountId2, "PAYMENT",
                parse("2018-09-23T09:46:01.123456Z"), FAILED_REJECTED, true, true, 200L, 0L);

        List<Map<String, Object>> transactionSummary = dbHelper.getTransactionSummary(gatewayAccountId1, PAYMENT,
                SUCCESS, parse("2018-09-22T00:00:00.000000Z"), false, false);
        assertThat(transactionSummary.get(0).get("gateway_account_id"), is(gatewayAccountId1));
        assertThat(transactionSummary.get(0).get("transaction_date").toString(), is("2018-09-22"));
        assertThat(transactionSummary.get(0).get("state"), is("SUCCESS"));
        assertThat(transactionSummary.get(0).get("live"), is(false));
        assertThat(transactionSummary.get(0).get("moto"), is(false));
        assertThat(transactionSummary.get(0).get("total_amount_in_pence"), is(100L));
        assertThat(transactionSummary.get(0).get("no_of_transactions"), is(1L));
        assertThat(transactionSummary.get(0).get("total_fee_in_pence"), is(10L));

        transactionSummary = dbHelper.getTransactionSummary(gatewayAccountId2, PAYMENT,
                FAILED_REJECTED, parse("2018-09-23T00:00:00.000000Z"), true, true);
        assertThat(transactionSummary.get(0).get("gateway_account_id"), is(gatewayAccountId2));
        assertThat(transactionSummary.get(0).get("transaction_date").toString(), is("2018-09-23"));
        assertThat(transactionSummary.get(0).get("state"), is("FAILED_REJECTED"));
        assertThat(transactionSummary.get(0).get("live"), is(true));
        assertThat(transactionSummary.get(0).get("moto"), is(true));
        assertThat(transactionSummary.get(0).get("total_amount_in_pence"), is(200L));
        assertThat(transactionSummary.get(0).get("no_of_transactions"), is(1L));
        assertThat(transactionSummary.get(0).get("total_fee_in_pence"), is(0L));
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
                false, false, 123L, 23L);

        List<Map<String, Object>> transactionSummary = dbHelper.getTransactionSummary(gatewayAccountId, PAYMENT,
                SUCCESS, parse("2018-09-22T00:00:00.000000Z"), false, false);
        assertThat(transactionSummary.get(0).get("gateway_account_id"), is(gatewayAccountId));
        assertThat(transactionSummary.get(0).get("transaction_date").toString(), is("2018-09-22"));
        assertThat(transactionSummary.get(0).get("state"), is("SUCCESS"));
        assertThat(transactionSummary.get(0).get("live"), is(false));
        assertThat(transactionSummary.get(0).get("moto"), is(false));
        assertThat(transactionSummary.get(0).get("total_amount_in_pence"), is(1123L));
        assertThat(transactionSummary.get(0).get("no_of_transactions"), is(11L));
        assertThat(transactionSummary.get(0).get("total_fee_in_pence"), is(100L));
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
}