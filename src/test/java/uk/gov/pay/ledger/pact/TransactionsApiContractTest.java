package uk.gov.pay.ledger.pact;

import au.com.dius.pact.provider.junit.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import java.time.ZonedDateTime;
import java.util.Map;

import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

@RunWith(PactRunner.class)
@Provider("ledger")
@IgnoreNoPactsToVerify
@PactBroker(scheme = "https", host = "pact-broker-test.cloudapps.digital", tags = {"${PACT_CONSUMER_TAG}", "test", "staging", "production"},
        authentication = @PactBrokerAuth(username = "${PACT_BROKER_USERNAME}", password = "${PACT_BROKER_PASSWORD}"),
        consumers = {"publicapi"})
public class TransactionsApiContractTest {

    @ClassRule
    public static AppWithPostgresAndSqsRule app = new AppWithPostgresAndSqsRule();

    @TestTarget
    public static Target target;

    @BeforeClass
    public static void setup() {
        target = new HttpTarget(app.getAppRule().getLocalPort());
    }

    @State("a transaction with created state exist")
    public void createChargeWithCardDetails(Map<String, String> params) {
        String transactionExternalId = params.get("transaction_external_id");
        String gatewayAccountId = params.get("gateway_account_id");

        TransactionFixture transactionFixture
                = aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withAmount(1000l)
                .withReference("aReference")
                .withDescription("Test description")
                .withState("created")
                .withReturnUrl("aReturnUrl")
                .withCreatedDate(ZonedDateTime.parse("2018-09-22T10:13:16.067Z"))
                .insert(app.getJdbi());
    }

}
