package uk.gov.pay.ledger.pact;

import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.payout.state.PayoutState;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.search.model.RefundSummary;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.AgreementFixture;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.agreement.AgreementStatus;

import java.time.ZonedDateTime;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.platform.commons.util.StringUtils.isBlank;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;
import static uk.gov.pay.ledger.util.fixture.PayoutFixture.PayoutFixtureBuilder.aPayoutFixture;
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
        helper.truncateAllPayoutData();
    }

    @State("a payment transaction exists")
    public void aPaymentTransactionExists(Map<String, String> params) {
        String transactionExternalId = params.get("transaction_external_id");
        String gatewayAccountId = params.get("gateway_account_id");
        String cardholderName = params.get("cardholder_name");

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
                .withCardholderName(cardholderName)
                .withDefaultCardDetails()
                .withVersion3ds("2.1.0")
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

    @State("refund and dispute transactions for a transaction exist")
    public void createRefundAndDisputeTransactionsForATransaction(Map<String, String> params) {
        String transactionExternalId = params.get("transaction_external_id");
        String gatewayAccountId = params.get("gateway_account_id");

        createPaymentTransaction(transactionExternalId, gatewayAccountId);

        createARefundTransaction(transactionExternalId, gatewayAccountId, "refund-transaction-1d1",
                100L, "reference1", "description1",
                "2018-09-22T10:14:16.067Z", TransactionState.SUBMITTED);

        createARefundTransaction(transactionExternalId, gatewayAccountId, "refund-transaction-1d2",
                200L, "reference2", "description2",
                "2018-09-22T10:16:16.067Z", TransactionState.ERROR_GATEWAY);

        JsonObject transactionDetails = new JsonObject();
        transactionDetails.addProperty("amount", 1000L);
        transactionDetails.addProperty("gateway_account_id", gatewayAccountId);

        createDisputeTransaction("dispute-transaction", gatewayAccountId, transactionExternalId, transactionDetails.toString(),
                TransactionState.NEEDS_RESPONSE, null, 1000L, null, null);
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
                TransactionState.CREATED, "reference1", null,
                null, "2019-09-20T10:14:16.067Z");
        createPaymentTransactionForSelfserviceSearch(transactionExternalId2, gatewayAccountId,
                TransactionState.SUBMITTED, "reference2", "visa",
                "Visa", "2019-09-21T10:14:16.067Z");

        createARefundTransaction(transactionExternalId2, gatewayAccountId, "refund-transaction-id",
                150L, "reference", "description",
                "2019-09-19T10:14:16.067Z", TransactionState.SUCCESS);
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
                .withDefaultTransactionDetails()
                .insertTransactionAndTransactionMetadata(app.getJdbi());
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
                .withDefaultTransactionDetails()
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
                .withDefaultTransactionDetails()
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
                .withDefaultTransactionDetails()
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
                .withDefaultTransactionDetails()
                .insert(app.getJdbi());
    }

    @State("a transaction with 3ds version exists")
    public void createTransactionWithVersion3ds(Map<String, String> params) {
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
                .withRefundSummary(RefundSummary.ofValue("pending", 100L, 0L))
                .withVersion3ds("2.1.0")
                .withDefaultTransactionDetails()
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
                .withDefaultTransactionDetails()
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

    @State("dispute transactions exist with states `lost` and `under_review` for selfservice search")
    public void createDisputeTransactionsForSearch() {
        String gatewayAccountId = "123456";
        String disputeLostExternalId = "duslqp12kpdfskopek230";
        String disputeUnderReviewExternalId = "du2slqp12kpdfskopek230";
        String paymentExternalId = "q5qo9mt6ajfcn2oqgaktkm2ksk";
        String paymentExternalId2 = "dklpej3vlkn2oqgaktkm2ksk";

        aTransactionFixture()
                .withExternalId(paymentExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(app.getJdbi()).toEntity();
        aTransactionFixture()
                .withExternalId(paymentExternalId2)
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(app.getJdbi()).toEntity();

        JsonObject transactionDetails = new JsonObject();
        transactionDetails.addProperty("amount", 1000L);
        transactionDetails.addProperty("gateway_account_id", gatewayAccountId);
        transactionDetails.addProperty("reason", "other");
        transactionDetails.addProperty("evidence_due_date", "2022-05-21T19:05:00Z");

        JsonObject paymentDetails = new JsonObject();
        paymentDetails.addProperty("expiry_date", "8/23");
        paymentDetails.addProperty("card_brand_label", "Visa");
        transactionDetails.add("payment_details", paymentDetails);

        createDisputeTransaction(disputeLostExternalId, gatewayAccountId, paymentExternalId, transactionDetails.toString(),
                TransactionState.LOST, null, 2000L, -3500L, 1500L);
        createDisputeTransaction(disputeUnderReviewExternalId, gatewayAccountId, paymentExternalId2, transactionDetails.toString(),
                TransactionState.UNDER_REVIEW, null, 1000L, null, null);
    }

    @State("a payment with success state exists")
    public void createAPaymentWithSuccessState(Map<String, String> params) {
        String gatewayAccountId = params.get("gateway_account_id");
        String createdDate = params.get("created_date");
        String reference = params.get("reference");
        String email = params.get("email");
        if (isBlank(gatewayAccountId)) {
            gatewayAccountId = "123456";
        }
        if (isBlank(createdDate)) {
            createdDate = "2019-05-03T00:00:01.000Z";
        }
        if (isBlank(reference)) {
            reference = "payment1";
        }
        if (isBlank(email)) {
            email = "j.doe@example.org";
        }
        String transactionExternalId = randomAlphanumeric(12);
        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withAmount(1000L)
                .withReference(reference)
                .withState(TransactionState.SUCCESS)
                .withDefaultCardDetails()
                .withFirstDigitsCardNumber("424242")
                .withLastDigitsCardNumber("4242")
                .withCardBrand("visa")
                .withCardBrandLabel("Visa")
                .withGatewayTransactionId("gateway-transaction-id")
                .withCardholderName("J Doe")
                .withEmail(email)
                .withCreatedDate(ZonedDateTime.parse(createdDate))
                .withCaptureSubmittedDate(ZonedDateTime.parse(createdDate).plusMinutes(20L))
                .withCapturedDate(ZonedDateTime.parse(createdDate).plusHours(1L))
                .withDefaultTransactionDetails()
                .insert(app.getJdbi());
    }

    @State("a dispute lost transaction exists")
    public void createADisputeTransaction(Map<String, String> params) {
        String transactionId = params.getOrDefault("transaction_external_id", "vldb123def456");
        String gatewayAccountId = params.getOrDefault("gateway_account_id", "123456");
        String parentTransactionExternalId = params.getOrDefault("parent_external_id", "adb123def456");

        String gatewayPayoutId = randomAlphanumeric(15);

        aTransactionFixture()
                .withExternalId(parentTransactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withCreatedDate(ZonedDateTime.parse("2022-05-10T14:05:00Z"))
                .withGatewayPayoutId(gatewayPayoutId)
                .withDefaultTransactionDetails()
                .insert(app.getJdbi()).toEntity();

        aPayoutFixture()
                .withGatewayPayoutId(gatewayPayoutId)
                .withGatewayAccountId(gatewayAccountId)
                .withPaidOutDate(ZonedDateTime.parse("2022-05-27T19:05:00Z"))
                .build()
                .insert(app.getJdbi());

        JsonObject transactionDetails = new JsonObject();
        transactionDetails.addProperty("amount", 1000L);
        transactionDetails.addProperty("gateway_account_id", gatewayAccountId);
        transactionDetails.addProperty("reason", "fraudulent");
        transactionDetails.addProperty("evidence_due_date", "2022-05-27T19:05:00Z");

        JsonObject paymentDetails = new JsonObject();
        paymentDetails.addProperty("expiry_date", "8/23");
        paymentDetails.addProperty("card_brand_label", "Visa");
        paymentDetails.addProperty("card_type", "CREDIT");
        transactionDetails.add("payment_details", paymentDetails);

        createDisputeTransaction(transactionId, gatewayAccountId, parentTransactionExternalId, transactionDetails.toString(),
                TransactionState.LOST, gatewayPayoutId, 1000L, -2500L, 1500L);
    }

    @State("a payment with all fields and a corresponding refund exists")
    public void createAPaymentWithAllFieldsAndWithACorrespondingRefund(Map<String, String> params) {
        String gatewayAccountId = params.get("gateway_account_id");
        if (isBlank(gatewayAccountId)) {
            gatewayAccountId = "123456";
        }
        String transactionExternalId = "someExternalId1";

        createPaymentTransactionWithAllFields(transactionExternalId, gatewayAccountId,
                TransactionState.SUCCESS, "reference1", "2019-09-21T12:22:16.067Z");

        createARefundTransaction(transactionExternalId, gatewayAccountId, "refund-transaction-id",
                150L, "reference2", "description",
                "2019-09-21T15:14:16.067Z", TransactionState.SUCCESS);

    }

    @State("two payouts exist for selfservice search")
    public void createTwoPayouts() {
        String gatewayAccountId = "654321";
        aPayoutFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId("payout-id-1")
                .withAmount(1250L)
                .withCreatedDate(ZonedDateTime.parse("2020-05-21T12:22:16.067Z"))
                .withPaidOutDate(ZonedDateTime.parse("2020-05-22T14:22:16.067Z"))
                .withState(PayoutState.PAID_OUT)
                .build()
                .insert(app.getJdbi());

        aPayoutFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId("payout-id-2")
                .withAmount(2345L)
                .withCreatedDate(ZonedDateTime.parse("2020-05-22T12:22:16.067Z"))
                .withPaidOutDate(ZonedDateTime.parse("2020-05-23T14:22:16.067Z"))
                .withState(PayoutState.PAID_OUT)
                .build()
                .insert(app.getJdbi());
    }

    @State("a payment with payout date exists")
    public void createAPaymentWithPayoutDate(Map<String, String> params) {
        String gatewayAccountId = params.get("account_id");
        String transactionExternalId = params.get("charge_id");
        String createdDate = params.get("created_date");
        String settledDate = params.get("settled_date");
        if (isBlank(gatewayAccountId)) {
            gatewayAccountId = "123456";
        }
        if (isBlank(createdDate)) {
            createdDate = "2019-05-03T00:00:01.000Z";
        }
        if (isBlank(settledDate)) {
            settledDate = "2019-05-06T00:00:01.000Z";
        }
        if (isBlank(transactionExternalId)) {
            transactionExternalId = "ch_123abc456settlement";
        }

        String gatewayPayoutId = randomAlphanumeric(15);

        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withDefaultCardDetails()
                .withCreatedDate(ZonedDateTime.parse(createdDate))
                .withCaptureSubmittedDate(ZonedDateTime.parse(createdDate).plusMinutes(20L))
                .withCapturedDate(ZonedDateTime.parse(createdDate).plusHours(1L))
                .withGatewayPayoutId(gatewayPayoutId)
                .withDefaultTransactionDetails()
                .insert(app.getJdbi());

        aPayoutFixture()
                .withPaidOutDate(ZonedDateTime.parse(settledDate))
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId)
                .build().insert(app.getJdbi());
    }

    @State("three payments with payout dates exists")
    public void createThreePaymentsWithPaidoutDates(Map<String, String> params) {
        String gatewayAccountId = params.get("account_id");
        if (isBlank(gatewayAccountId)) {
            gatewayAccountId = "123456";
        }
        String gatewayPayoutId1 = randomAlphanumeric(20);
        String gatewayPayoutId2 = randomAlphanumeric(20);
        String gatewayPayoutId3 = randomAlphanumeric(20);
        String gatewayPayoutId4 = randomAlphanumeric(20);

        TransactionFixture refundParentFixture = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId1)
                .insert(app.getJdbi());
        aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId2)
                .insert(app.getJdbi());
        aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId3)
                .insert(app.getJdbi());
        aTransactionFixture()
                .withTransactionType("REFUND")
                .withParentExternalId(refundParentFixture.getExternalId())
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId4)
                .insert(app.getJdbi());

        aPayoutFixture()
                .withGatewayPayoutId(gatewayPayoutId1)
                .withGatewayAccountId(gatewayAccountId)
                .withPaidOutDate(ZonedDateTime.parse("2020-09-19T10:15:30Z"))
                .build()
                .insert(app.getJdbi());
        aPayoutFixture()
                .withGatewayPayoutId(gatewayPayoutId2)
                .withGatewayAccountId(gatewayAccountId)
                .withPaidOutDate(ZonedDateTime.parse("2020-09-18T23:59:59.999Z"))
                .build()
                .insert(app.getJdbi());
        aPayoutFixture()
                .withGatewayPayoutId(gatewayPayoutId3)
                .withGatewayAccountId(gatewayAccountId)
                .withPaidOutDate(ZonedDateTime.parse("2020-09-21T00:00:00Z"))
                .build()
                .insert(app.getJdbi());
        aPayoutFixture()
                .withGatewayPayoutId(gatewayPayoutId4)
                .withGatewayAccountId(gatewayAccountId)
                .withPaidOutDate(ZonedDateTime.parse("2020-09-19T19:05:00Z"))
                .build()
                .insert(app.getJdbi());
    }

    @State("3 agreements exist for account")
    public void agreementsExist(Map<String, String> params) {
        String accountId = params.get("account_id");

        AgreementFixture.anAgreementFixture()
                .withGatewayAccountId(accountId)
                .withExternalId("agreement-1")
                .withStatus(AgreementStatus.CREATED)
                .insert(app.getJdbi());
        AgreementFixture.anAgreementFixture()
                .withGatewayAccountId(accountId)
                .withExternalId("agreement-2")
                .withStatus(AgreementStatus.CREATED)
                .insert(app.getJdbi());
        AgreementFixture.anAgreementFixture()
                .withGatewayAccountId(accountId)
                .withExternalId("agreement-3")
                .withStatus(AgreementStatus.ACTIVE)
                .insert(app.getJdbi());
    }

    @State("a recurring card payment exists for agreement")
    public void recurringCardPaymentExistsForAgreement(Map<String, String> params) {
        String accountId = params.get("account_id");
        String agreementId = params.get("agreement_id");

        TransactionFixture.aTransactionFixture()
                .withGatewayAccountId(accountId)
                .withAgreementId(agreementId)
                .withAuthorisationMode(AuthorisationMode.AGREEMENT)
                .withDefaultTransactionDetails()
                .insert(app.getJdbi());
        TransactionFixture.aTransactionFixture()
                .withGatewayAccountId(accountId)
                .withAgreementId("other-agreement-id")
                .withAuthorisationMode(AuthorisationMode.AGREEMENT)
                .withDefaultTransactionDetails()
                .insert(app.getJdbi());
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
                .withDefaultPaymentDetails()
                .withDefaultTransactionDetails()
                .insert(app.getJdbi());
    }

    private void createPaymentTransaction(String transactionExternalId, String gatewayAccountId) {
        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withDefaultTransactionDetails()
                .insert(app.getJdbi()).toEntity();
    }

    private void createDisputeTransaction(String transactionExternalId, String gatewayAccountId,
                                          String parentExternalId, String transactionDetails,
                                          TransactionState transactionState, String gatewayPayoutId,
                                          Long amount, Long netAmount, Long fee) {
        TransactionFixture transactionFixture = aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withExternalId(transactionExternalId)
                .withAmount(amount)
                .withReference("my payment reference")
                .withDescription("Test payment")
                .withState(transactionState)
                .withEmail("joe.blogs@example.com")
                .withCardholderName("Mr Test")
                .withCreatedDate(ZonedDateTime.parse("2022-05-20T19:05:00Z"))
                .withTransactionDetails(transactionDetails)
                .withCardBrand("visa")
                .withLastDigitsCardNumber("4242")
                .withFirstDigitsCardNumber("424242")
                .withGatewayTransactionId("du_dl20kdldj20ejs103jns")
                .withTransactionType(TransactionType.DISPUTE.name())
                .withParentExternalId(parentExternalId)
                .withLive(true)
                .withMoto(false)
                .withGatewayPayoutId(gatewayPayoutId)
                .withServiceId("36806175a0f944ff8bc88f97634b38a2");

        if (netAmount != null) {
            transactionFixture.withNetAmount(netAmount);
        }
        if (fee != null) {
            transactionFixture.withFee(fee);
        }

        transactionFixture.insert(app.getJdbi()).toEntity();
    }

    private void createPaymentTransaction(String transactionExternalId, String gatewayAccountId, Long amount, TransactionState state, String createdDate) {
        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withAmount(amount)
                .withState(state)
                .withCreatedDate(ZonedDateTime.parse(createdDate))
                .withDefaultTransactionDetails()
                .insert(app.getJdbi()).toEntity();
    }

    private void createPaymentTransactionForSelfserviceSearch(String transactionExternalId,
                                                              String gatewayAccountId,
                                                              TransactionState state,
                                                              String reference,
                                                              String cardBrand,
                                                              String cardBrandLabel,
                                                              String createdDate) {
        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withReference(reference)
                .withCardholderName(null)
                .withState(state)
                .withCardBrand(cardBrand)
                .withCardBrandLabel(cardBrandLabel)
                .withCreatedDate(ZonedDateTime.parse(createdDate))
                .withDefaultTransactionDetails()
                .insert(app.getJdbi());
    }

    private void createPaymentTransactionWithAllFields(String transactionExternalId,
                                                       String gatewayAccountId,
                                                       TransactionState state,
                                                       String reference,
                                                       String createdDate) {
        aTransactionFixture()
                .withExternalId(transactionExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withAmount(1000L)
                .withReference(reference)
                .withState(state)
                .withDefaultCardDetails()
                .withFirstDigitsCardNumber("424242")
                .withLastDigitsCardNumber("1212")
                .withCardBrand("visa")
                .withCardBrandLabel("Visa")
                .withGatewayTransactionId("gateway-transaction-id")
                .withCardholderName("J Doe")
                .withEmail("test@example.org")
                .withCreatedDate(ZonedDateTime.parse(createdDate))
                .withCaptureSubmittedDate(ZonedDateTime.parse(createdDate).plusMinutes(2L))
                .withCapturedDate(ZonedDateTime.parse(createdDate).plusHours(1L))
                .withDefaultTransactionDetails()
                .insert(app.getJdbi());
    }
}
