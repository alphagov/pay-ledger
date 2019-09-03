package uk.gov.pay.ledger.pact;

import au.com.dius.pact.provider.junit.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junit.loader.PactFilter;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.util.Map;

import static org.junit.platform.commons.util.StringUtils.isBlank;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

@RunWith(PactRunner.class)
@Provider("ledger")
@PactBroker(scheme = "https", host = "pact-broker-test.cloudapps.digital", tags = {"${PACT_CONSUMER_TAG}", "test"},
        authentication = @PactBrokerAuth(username = "${PACT_BROKER_USERNAME}", password = "${PACT_BROKER_PASSWORD}"),
        consumers = {"publicapi", "selfservice"})
@PactFilter({"a transaction has CREATED and AUTHORISATION_REJECTED payment events"})
@IgnoreNoPactsToVerify
public class TransactionEventsApiContractTest {

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

    @State("a transaction has CREATED and AUTHORISATION_REJECTED payment events")
    public void createTransactionWithTwoEvents(Map<String, String> params) {
        String transactionId = params.get("transaction_id");
        String gatewayAccountId = params.get("gateway_account_id");

        if (isBlank(transactionId)) {
            transactionId = "ch_123abc456xyz";
        }
        if (isBlank(gatewayAccountId)) {
            gatewayAccountId = "123456";
        }

        aTransactionFixture()
                .withExternalId(transactionId)
                .withGatewayAccountId(gatewayAccountId)
                .insert(app.getJdbi());

        anEventFixture()
                .withEventType(SalientEventType.PAYMENT_CREATED.name())
                .withResourceExternalId(transactionId)
                .withEventData("{}")
                .insert(app.getJdbi());

        anEventFixture()
                .withEventType(SalientEventType.AUTHORISATION_REJECTED.name())
                .withResourceExternalId(transactionId)
                .withEventData("{}")
                .insert(app.getJdbi());
    }
}
