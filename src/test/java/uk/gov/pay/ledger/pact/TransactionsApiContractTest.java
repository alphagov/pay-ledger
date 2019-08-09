package uk.gov.pay.ledger.pact;

import au.com.dius.pact.provider.junit.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junit.loader.PactFilter;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.time.ZonedDateTime;
import java.util.Map;

import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

@RunWith(PactRunner.class)
@Provider("ledger")
@PactBroker(scheme = "https", host = "pact-broker-test.cloudapps.digital", tags = {"${PACT_CONSUMER_TAG}", "test", "staging", "production"},
        authentication = @PactBrokerAuth(username = "${PACT_BROKER_USERNAME}", password = "${PACT_BROKER_PASSWORD}"),
        consumers = {"publicapi"})
@PactFilter({"a transaction with created state exist",
        "refund transactions for a transaction exist"
})
@IgnoreNoPactsToVerify
public class TransactionsApiContractTest {

    @ClassRule
    public static AppWithPostgresAndSqsRule app = new AppWithPostgresAndSqsRule();

    @TestTarget
    public static Target target;

    @BeforeClass
    public static void setupClass() {
        target = new HttpTarget(app.getAppRule().getLocalPort());
    }

    @Before
    public void setUp() {
        DatabaseTestHelper helper = aDatabaseTestHelper(app.getJdbi());
        helper.truncateAllData();
    }

    @State("a transaction with created state exist")
    public void createTransactionWithCardDetails(Map<String, String> params) {
        String transactionExternalId = params.get("transaction_external_id");
        String gatewayAccountId = params.get("gateway_account_id");

        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withAmount(1000l)
                .withReference("aReference")
                .withDescription("Test description")
                .withState(TransactionState.CREATED)
                .withReturnUrl("https://example.org")
                .withCardBrand(null)
                .withDefaultCardDetails()
                .withDefaultTransactionDetails()
                .withCreatedDate(ZonedDateTime.parse("2018-09-22T10:13:16.067Z"))
                .insert(app.getJdbi());
    }

    @State("refund transactions for a transaction exist")
    public void createRefundTransactionForATransaction(Map<String, String> params) {
        String transactionExternalId = params.get("transaction_external_id");
        String gatewayAccountId = params.get("gateway_account_id");

        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .insert(app.getJdbi()).toEntity();

        aTransactionFixture()
                .withExternalId("refund-transaction-id1")
                .withParentExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withAmount(100L)
                .withTransactionType("REFUND")
                .withReference("reference1")
                .withDescription("description1")
                .withCreatedDate(ZonedDateTime.parse("2018-09-22T10:14:16.067Z"))
                .insert(app.getJdbi());

        aTransactionFixture()
                .withExternalId("refund-transaction-id2")
                .withParentExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withAmount(200L)
                .withTransactionType("REFUND")
                .withReference("reference2")
                .withDescription("description2")
                .withCreatedDate(ZonedDateTime.parse("2018-09-22T10:16:16.067Z"))
                .insert(app.getJdbi());
    }
}
