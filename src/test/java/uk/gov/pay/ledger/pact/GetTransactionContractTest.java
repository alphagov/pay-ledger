package uk.gov.pay.ledger.pact;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.search.model.RefundSummary;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.time.ZonedDateTime;
import java.util.Map;

import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

@RunWith(PactRunner.class)
@Provider("ledger")
@PactFolder("/Users/mariajankowiak/repostitories/pay/pay-ledger/src/test/resources/pacts")
public class GetTransactionContractTest {
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

    @State("a transaction with metadata exists")
    public void createTransactionWithMetadata(Map<String, String> params) {
        String transactionExternalId = params.get("charge_id");
        String metadata = params.get("metadata");

        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withState(TransactionState.CREATED)
                .withAmount(100L)
                .withExternalMetadata(metadata)
                .insert(app.getJdbi());
    }

    @State("a transaction with a gateway transaction id exists")
    public void createTransactionWithGatewayId(Map<String, String> params) {
        String transactionExternalId = params.get("charge_id");
        String gatewayTransactionId = params.get("gateway_transaction_id");

        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withState(TransactionState.CREATED)
                .withAmount(100L)
                .withGatewayTransactionId(gatewayTransactionId)
                .insert(app.getJdbi());
    }

    @State("a transaction with corporate surcharge exists")
    public void createTransactionWithCorporateSurcharge(Map<String, String> params) {
        String transactionExternalId = params.get("charge_id");

        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withState(TransactionState.CREATED)
                .withAmount(2000L)
                .withCorporateCardSurcharge(250L)
                .withTotalAmount(2250L)
                .withSettlementSubmittedTime(ZonedDateTime.now())
                .withSettledTime(ZonedDateTime.now())
                .insert(app.getJdbi());
    }

    @State("a transaction with fee and net_amount exists")
    public void createTransactionWithFeeAndNetAmount(Map<String, String> params) {
        String transactionExternalId = params.get("charge_id");

        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withState(TransactionState.CREATED)
                .withAmount(100L)
                .withFee(5L)
                .withNetAmount(95L)
                .insert(app.getJdbi());
    }

    @State("a transaction with delayed capture true exists")
    public void createTransactionWithDelayedCapture(Map<String, String> params) {
        String transactionExternalId = params.get("charge_id");

        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withState(TransactionState.CREATED)
                .withDelayedCapture(true)
                .withRefundSummary(RefundSummary.ofValue("pending", 100L, 0L))
                .withAmount(100L)
                .insert(app.getJdbi());
    }
}
