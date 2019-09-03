package uk.gov.pay.ledger.pact;

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
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.platform.commons.util.StringUtils.isBlank;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

@RunWith(PactRunner.class)
@Provider("ledger")
@PactBroker(scheme = "https", host = "pact-broker-test.cloudapps.digital", tags = {"${PACT_CONSUMER_TAG}", "test"},
        authentication = @PactBrokerAuth(username = "${PACT_BROKER_USERNAME}", password = "${PACT_BROKER_PASSWORD}"),
        consumers = {"publicapi", "selfservice"})
@PactFilter({"a transaction with created state exist",
        "a refund transaction for a transaction exists",
        "refund transactions for a transaction exist",
        "refund transactions exists for a gateway account",
        "two payments and a refund transactions exist for selfservice search"
})
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
                .withAmount(1000L)
                .withReference("aReference")
                .withDescription("Test description")
                .withState(TransactionState.CREATED)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withReturnUrl("https://example.org")
                .withCardBrand(null)
                .withDefaultCardDetails()
                .withDefaultTransactionDetails()
                .withCreatedDate(ZonedDateTime.parse("2018-09-22T10:13:16.067Z"))
                .insert(app.getJdbi());
    }

    @State("a refund transaction for a transaction exists")
    public void createRefundTransactionForATransaction(Map<String, String> params) {
        String transactionExternalId = params.get("transaction_external_id");
        String parentExternalId = params.get("parent_external_id");
        String gatewayAccountId = params.get("gateway_account_id");

        createPaymentTransaction(parentExternalId, gatewayAccountId);

        createARefundTransaction(parentExternalId, gatewayAccountId, transactionExternalId,
                100L, "reference1", "description1",
                "2018-09-22T10:14:16.067Z", TransactionState.SUCCESS);
    }

    @State("refund transactions for a transaction exist")
    public void createRefundTransactionsForATransaction(Map<String, String> params) {
        String transactionExternalId = params.get("transaction_external_id");
        String gatewayAccountId = params.get("gateway_account_id");

        createPaymentTransaction(transactionExternalId, gatewayAccountId);

        createARefundTransaction(transactionExternalId, gatewayAccountId, "refund-transaction-1d1",
                100L, "reference1", "description1",
                "2018-09-22T10:14:16.067Z", TransactionState.SUBMITTED);

        createARefundTransaction(transactionExternalId, gatewayAccountId, "refund-transaction-1d2",
                200L, "reference2", "description2",
                "2018-09-22T10:16:16.067Z", TransactionState.ERROR_GATEWAY);
    }

    @State("refund transactions exists for a gateway account")
    public void createRefundTransactionsForAGatewayAccount(Map<String, String> params) {
        String transactionExternalId1 = "someExternalId1";
        String transactionExternalId2 = "someExternalId2";
        String gatewayAccountId = params.get("gateway_account_id");

        if (isBlank(gatewayAccountId)) {
            gatewayAccountId = "123456";
        }

        createPaymentTransaction(transactionExternalId1, gatewayAccountId);
        createPaymentTransaction(transactionExternalId2, gatewayAccountId);

        createARefundTransaction(transactionExternalId1, gatewayAccountId, "refund-transaction-1d1",
                150L, "reference1", null,
                "2018-09-22T10:14:16.067Z", TransactionState.SUCCESS);

        createARefundTransaction(transactionExternalId2, gatewayAccountId, "refund-transaction-1d2",
                250L, "reference2", null,
                "2018-10-22T10:16:16.067Z", TransactionState.SUCCESS);
    }

    @State("two payments and a refund transactions exist for selfservice search")
    public void createThreeTransactionsForSelfserviceSearch() {
        String gatewayAccountId = "123456";
        String transactionExternalId1 = "someExternalId1";
        String transactionExternalId2 = "someExternalId2";

        createPaymentTransactionForSelfserviceSearch(transactionExternalId1, gatewayAccountId,
                TransactionState.CREATED, "reference1", null);
        createPaymentTransactionForSelfserviceSearch(transactionExternalId2, gatewayAccountId,
                TransactionState.SUBMITTED, "reference2", "visa");

        createARefundTransaction(transactionExternalId2, gatewayAccountId, "refund-transaction-id",
                150L, "reference", "description",
                "2018-09-22T10:14:16.067Z", TransactionState.SUCCESS);
    }

    private void createARefundTransaction(String parentExternalId, String gatewayAccountId,
                                          String externalId, Long amount,
                                          String reference, String description,
                                          String createdDate, TransactionState state) {
        aTransactionFixture()
                .withExternalId(externalId)
                .withParentExternalId(parentExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withAmount(amount)
                .withState(state)
                .withTransactionType(TransactionType.REFUND.name())
                .withReference(reference)
                .withDescription(description)
                .withCreatedDate(ZonedDateTime.parse(createdDate))
                .insert(app.getJdbi());
    }

    private void createPaymentTransaction(String transactionExternalId, String gatewayAccountId) {
        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .insert(app.getJdbi()).toEntity();
    }

    private void createPaymentTransactionForSelfserviceSearch(String transactionExternalId,
                                                              String gatewayAccountId,
                                                              TransactionState state,
                                                              String reference,
                                                              String cardBrand) {
        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withReference(reference)
                .withCardholderName(null)
                .withState(state)
                .withCardBrand(cardBrand)
                .insert(app.getJdbi());
    }
}
