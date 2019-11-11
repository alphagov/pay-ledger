package uk.gov.pay.ledger.pact;

import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.search.model.RefundSummary;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.time.ZonedDateTime;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.platform.commons.util.StringUtils.isBlank;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public abstract class ContractTest {

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
                TransactionState.CREATED, "reference1", null, null);
        createPaymentTransactionForSelfserviceSearch(transactionExternalId2, gatewayAccountId,
                TransactionState.SUBMITTED, "reference2", "visa", "Visa");

        createARefundTransaction(transactionExternalId2, gatewayAccountId, "refund-transaction-id",
                150L, "reference", "description",
                "2018-09-22T10:14:16.067Z", TransactionState.SUCCESS);
    }

    @State("a transaction with metadata exists")
    public void createTransactionWithMetadata(Map<String, String> params) {
        String transactionExternalId = params.get("charge_id");
        String gatewayAccountId = params.get("account_id");
        String metadata = params.get("metadata");

        if (isBlank(transactionExternalId)) {
            transactionExternalId = "ch_123abc456xyz";
        }
        if (isBlank(gatewayAccountId)) {
            gatewayAccountId = "123456";
        }
        if (isBlank(metadata)) {
            metadata = new GsonBuilder().create()
                    .toJson(ImmutableMap.of("external_metadata", "metadata"));
        }

        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withState(TransactionState.CREATED)
                .withAmount(100L)
                .withExternalMetadata(metadata)
                .insert(app.getJdbi());
    }

    @State("a transaction with a gateway transaction id exists")
    public void createTransactionWithGatewayId(Map<String, String> params) {
        String transactionExternalId = params.get("charge_id");
        String gatewayAccountId = params.get("account_id");
        String gatewayTransactionId = params.get("gateway_transaction_id");

        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withState(TransactionState.CREATED)
                .withAmount(100L)
                .withGatewayTransactionId(gatewayTransactionId)
                .insert(app.getJdbi());
    }

    @State("a transaction with corporate surcharge exists")
    public void createTransactionWithCorporateSurcharge(Map<String, String> params) {
        String transactionExternalId = params.get("charge_id");
        String gatewayAccountId = params.get("account_id");

        if (isBlank(transactionExternalId)) {
            transactionExternalId = "ch_123abc456xyz";
        }
        if (isBlank(gatewayAccountId)) {
            gatewayAccountId = "123456";
        }

        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withState(TransactionState.CREATED)
                .withAmount(2000L)
                .withCorporateCardSurcharge(250L)
                .withTotalAmount(2250L)
                .withCaptureSubmittedDate(ZonedDateTime.now())
                .withCapturedDate(ZonedDateTime.now())
                .insert(app.getJdbi());
    }

    @State("a transaction with fee and net_amount exists")
    public void createTransactionWithFeeAndNetAmount(Map<String, String> params) {
        String transactionExternalId = params.get("charge_id");
        String gatewayAccountId = params.get("account_id");

        if (isBlank(transactionExternalId)) {
            transactionExternalId = "ch_123abc456xyz";
        }
        if (isBlank(gatewayAccountId)) {
            gatewayAccountId = "123456";
        }

        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withState(TransactionState.CREATED)
                .withAmount(100L)
                .withFee(5L)
                .withNetAmount(95L)
                .insert(app.getJdbi());
    }

    @State("a transaction with delayed capture true exists")
    public void createTransactionWithDelayedCapture(Map<String, String> params) {
        String transactionExternalId = params.get("charge_id");
        String gatewayAccountId = params.get("account_id");

        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withState(TransactionState.CREATED)
                .withDelayedCapture(true)
                .withRefundSummary(RefundSummary.ofValue("pending", 100L, 0L))
                .withAmount(100L)
                .insert(app.getJdbi());
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

    @State("three payments and a refund all in success state exists")
    public void createThreeTransactionsAndARefundInSuccessState(Map<String, String> params) {
        String gatewayAccountId = params.get("gateway_account_id");
        if (isBlank(gatewayAccountId)) {
            gatewayAccountId = "123456";
        }
        String externalId1 = randomAlphanumeric(12);
        createPaymentTransaction(externalId1, gatewayAccountId, 1000L, TransactionState.SUCCESS, "2019-09-19T09:05:16.067Z");
        createPaymentTransaction(randomAlphanumeric(12), gatewayAccountId, 2000L, TransactionState.SUCCESS, "2019-09-19T19:06:16.067Z");
        createPaymentTransaction(randomAlphanumeric(12), gatewayAccountId, 1500L, TransactionState.SUCCESS, "2019-09-19T19:10:16.067Z");
        createARefundTransaction(externalId1, gatewayAccountId, randomAlphanumeric(12), 1000L, "reference", "description",
                "2019-09-21T19:05:16.067Z", TransactionState.SUCCESS);

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

    private void createPaymentTransaction(String transactionExternalId, String gatewayAccountId, Long amount, TransactionState state, String createdDate) {
        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withAmount(amount)
                .withState(state)
                .withCreatedDate(ZonedDateTime.parse(createdDate))
                .insert(app.getJdbi()).toEntity();
    }

    private void createPaymentTransactionForSelfserviceSearch(String transactionExternalId,
                                                              String gatewayAccountId,
                                                              TransactionState state,
                                                              String reference,
                                                              String cardBrand,
                                                              String cardBrandLabel) {
        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withReference(reference)
                .withCardholderName(null)
                .withState(state)
                .withCardBrand(cardBrand)
                .withCardBrandLabel(cardBrandLabel)
                .insert(app.getJdbi());
    }
}
