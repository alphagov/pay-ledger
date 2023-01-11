package uk.gov.pay.ledger.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.ReportingConfig;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.transaction.model.TransactionType.PAYMENT;
import static uk.gov.pay.ledger.transaction.state.TransactionState.SUCCESS;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.QueueDisputeEventFixture.aQueueDisputeEventFixture;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class QueueMessageReceiverIT {

    @Mock
    LedgerConfig ledgerConfig;
    @Mock
    ReportingConfig reportingConfig;

    private GsonBuilder gsonBuilder = new GsonBuilder();

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    TransactionDao transactionDao = new TransactionDao(rule.getJdbi(), ledgerConfig);
    private DatabaseTestHelper dbHelper = aDatabaseTestHelper(rule.getJdbi());

    private static final ZonedDateTime CREATED_AT = ZonedDateTime.parse("2019-06-07T08:46:01.123456Z");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        when(reportingConfig.getSearchQueryTimeoutInSeconds()).thenReturn(50);
        when(ledgerConfig.getReportingConfig()).thenReturn(reportingConfig);
        dbHelper.truncateAllData();
    }

    @Test
    public void shouldHandleOutOfOrderEvents() throws InterruptedException {
        final String resourceExternalId = "rexid";
        String gatewayAccountId = "test_gateway_account_id";
        aQueuePaymentEventFixture()
                .withLive(null)
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("AUTHORISATION_SUCCEEDED")
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        // A created event with an earlier timestamp, sent later
        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT.minusMinutes(1))
                .withGatewayAccountId(gatewayAccountId)
                .withEventType(SalientEventType.PAYMENT_CREATED.name())
                .withDefaultEventDataForEventType(SalientEventType.PAYMENT_CREATED.name())
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(200)
                .body("transaction_id", is(resourceExternalId))
                .body("state.status", is("submitted"));
    }

    @Test
    public void shouldSetLiveFromEventDetailsIfNotSetAtTopLevel() throws InterruptedException {
        final String resourceExternalId = "rexid";
        String gatewayAccountId = "test_gateway_account_id";

        aQueuePaymentEventFixture()
                .withLive(null)
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType(SalientEventType.CAPTURE_CONFIRMED_BY_GATEWAY_NOTIFICATION.name())
                .withEventData(format("{\"gateway_account_id\":\"%s\", \"live\": true}", gatewayAccountId))
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(200)
                .body("live", is(true));
    }

    @Test
    public void shouldContinueToHandleMessagesFromQueueForDownstreamExceptions() throws InterruptedException {
        final String resourceExternalId = "rexid";
        final String resourceExternalId2 = "rexid2";
        String gatewayAccountId = "test_gateway_account_id";

        aQueuePaymentEventFixture()
                .withResourceType(ResourceType.SERVICE) // throws PSQL exception. change to UNKNOWN when 'service' events are allowed
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("AUTHORISATION_SUCCEEDED")
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        QueuePaymentEventFixture secondResource = aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId2)
                .withResourceType(ResourceType.PAYMENT)
                .withEventDate(CREATED_AT.minusMinutes(1))
                .withEventType(SalientEventType.PAYMENT_CREATED.name())
                .withDefaultEventDataForEventType(SalientEventType.PAYMENT_CREATED.name())
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(404);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId2 + "?account_id=" + secondResource.getGatewayAccountId())
                .then()
                .statusCode(200)
                .body("transaction_id", is(resourceExternalId2))
                .body("state.status", is("created"));
    }

    @Test
    public void shouldHandleRefundEvent() throws InterruptedException {
        final String resourceExternalId = "rexid";
        final String parentResourceExternalId = "parentRexId";
        final String gatewayAccountId = "test_accountId";

        aQueuePaymentEventFixture()
                .withResourceExternalId(parentResourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("PAYMENT_CREATED")
                .withDefaultEventDataForEventType("PAYMENT_CREATED")
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId(parentResourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("PAYMENT_DETAILS_ENTERED")
                .withDefaultEventDataForEventType("PAYMENT_DETAILS_ENTERED")
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId(parentResourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("AUTHORISATION_SUCCEEDED")
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        Thread.sleep(100);
        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withParentResourceExternalId(parentResourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("REFUND_CREATED_BY_SERVICE")
                .withResourceType(ResourceType.REFUND)
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(200)
                .body("transaction_id", is(resourceExternalId))
                .body("state.status", is("submitted"));

        Optional<TransactionEntity> mayBeRefund = transactionDao.findTransactionByExternalId(resourceExternalId);
        TransactionEntity refund = mayBeRefund.get();
        assertThat(refund.getCardholderName(), is("J citizen"));
        assertThat(refund.getEmail(), is("j.doe@example.org"));
        assertThat(refund.getCardBrand(), is("visa"));
        assertThat(refund.getDescription(), is("a description"));
        assertThat(refund.getLastDigitsCardNumber(), is("4242"));
        assertThat(refund.getFirstDigitsCardNumber(), is("424242"));
        assertThat(refund.getReference(), is("aref"));
        Map<String, Object> transactionDetails = new Gson().fromJson(refund.getTransactionDetails(), Map.class);
        Map<String, String> paymentDetails = (Map<String, String>) transactionDetails.get("payment_details");

        assertThat(paymentDetails.get("card_brand_label"), is("Visa"));
        assertThat(paymentDetails.get("expiry_date"), is("11/21"));
        assertThat(paymentDetails.get("card_type"), is("DEBIT"));
    }

    @Test
    public void shouldHandleRefundAvailabilityUpdatedEvent() throws InterruptedException {
        final String resourceExternalId = "rexid2";
        final String gatewayAccountId = "test_accountId";
        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("AUTHORISATION_SUCCEEDED")
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("REFUND_AVAILABILITY_UPDATED")
                .withEventData("{\"refund_amount_available\": 200}")
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(200)
                .body("transaction_id", is(resourceExternalId))
                .body("refund_summary.amount_available", is(200));
    }

    @Test
    public void shouldHandleForceCaptureEvent() throws InterruptedException {
        final String resourceExternalId = "rexid3";
        final String gatewayAccountId = "test_accountId";
        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("AUTHORISATION_REJECTED")
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(200)
                .body("transaction_id", is(resourceExternalId))
                .body("state.status", is("declined"));

        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("CAPTURE_CONFIRMED_BY_GATEWAY_NOTIFICATION")
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(200)
                .body("transaction_id", is(resourceExternalId))
                .body("state.status", is("success"));

    }

    @Test
    public void shouldProjectTransactionSummaryForPaymentEvents() throws InterruptedException {
        final String resourceExternalId = "rexid" + randomAlphanumeric(10);
        final String gatewayAccountId = "test_accountId" + randomAlphanumeric(10);

        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT.plusSeconds(1))
                .withEventType("CAPTURE_SUBMITTED")
                .withEventData("{}")
                .withDefaultEventDataForEventType("DEFAULT")
                .withLive(true)
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT.plusSeconds(2))
                .withEventType("CAPTURE_CONFIRMED")
                .withDefaultEventDataForEventType("CAPTURE_CONFIRMED")
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withEventDate(CREATED_AT)
                .withEventType("PAYMENT_CREATED")
                .withDefaultEventDataForEventType("PAYMENT_CREATED")
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        List<Map<String, Object>> transactionSummary = dbHelper.getTransactionSummary(gatewayAccountId, PAYMENT, SUCCESS, LocalDate.parse("2019-06-07"), true, false);

        assertThat(transactionSummary.get(0).get("gateway_account_id"), is(gatewayAccountId));
        assertThat(transactionSummary.get(0).get("transaction_date").toString(), Matchers.is("2019-06-07"));
        assertThat(transactionSummary.get(0).get("state"), is("SUCCESS"));
        assertThat(transactionSummary.get(0).get("live"), is(true));
        assertThat(transactionSummary.get(0).get("moto"), is(false));
        assertThat(transactionSummary.get(0).get("total_amount_in_pence"), is(1000L));
        assertThat(transactionSummary.get(0).get("no_of_transactions"), is(1L));
        assertThat(transactionSummary.get(0).get("total_fee_in_pence"), is(5L));
    }

    @Test
    public void shouldHandleDisputeTypeEvent() throws InterruptedException, JsonProcessingException {
        final String resourceExternalId = "rexid";
        final String parentResourceExternalId = "parentRexId";
        final String gatewayAccountId = "test_accountId";

        aQueuePaymentEventFixture()
                .withResourceExternalId(parentResourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("PAYMENT_CREATED")
                .withLive(false)
                .withDefaultEventDataForEventType("PAYMENT_CREATED")
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId(parentResourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("PAYMENT_DETAILS_ENTERED")
                .withLive(false)
                .withDefaultEventDataForEventType("PAYMENT_DETAILS_ENTERED")
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId(parentResourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("AUTHORISATION_SUCCEEDED")
                .withLive(false)
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId(parentResourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("FEE_INCURRED")
                .withLive(false)
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "net_amount", 900,
                                "fee", 100
                        )))
                .insert(rule.getSqsClient());

        Thread.sleep(100);

        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withParentResourceExternalId(parentResourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("DISPUTE_CREATED")
                .withResourceType(ResourceType.DISPUTE)
                .withLive(false)
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "evidence_due_date",  Instant.now().getEpochSecond(),
                                "gateway_account_id", gatewayAccountId,
                                "amount", 50,
                                "reason", "fraudulent"
                        )))
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        // TODO: Replace this once the transaction endpoint is returning disputes - https://payments-platform.atlassian.net/browse/PP-9627
        Map<String, Object> dispute = dbHelper.getTransaction(resourceExternalId);

        assertThat(dispute, hasEntry("state", "NEEDS_RESPONSE"));
        assertThat(dispute, hasEntry("amount", 50L));
        assertThat(dispute, hasEntry("gateway_account_id", gatewayAccountId));
        assertThat(dispute, hasEntry("fee", null));
        assertThat(dispute, hasEntry("net_amount", null));

        assertThat(dispute, hasEntry("cardholder_name", "J citizen"));
        assertThat(dispute, hasEntry("email", "j.doe@example.org"));
        assertThat(dispute, hasEntry("card_brand", "visa"));
        assertThat(dispute, hasEntry("description", "a description"));
        assertThat(dispute, hasEntry("last_digits_card_number", "4242"));
        assertThat(dispute, hasEntry("first_digits_card_number", "424242"));
        assertThat(dispute, hasEntry("reference", "aref"));
    }

    @Test
    public void shouldHandleAgreementEvent() throws InterruptedException {
        aQueuePaymentEventFixture()
                .withResourceExternalId("a-valid-agreement-id")
                .withResourceType(ResourceType.AGREEMENT)
                .withEventDate(CREATED_AT)
                .withEventType("AGREEMENT_CREATED")
                .withEventData(
                        gsonBuilder.create()
                                .toJson(Map.of(
                                        "reference", "agreement-reference",
                                        "description", "agreement description text",
                                        "status", "CREATED"
                                ))
                )
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId("a-valid-agreement-id")
                .withResourceType(ResourceType.AGREEMENT)
                .withEventDate(CREATED_AT.plusMinutes(10))
                .withEventType("AGREEMENT_SETUP")
                .withEventData(
                        gsonBuilder.create()
                                .toJson(Map.of(
                                        "status", "ACTIVE",
                                        "payment_instrument_external_id", "a-valid-instrument-id"
                                ))
                )
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .queryParam("override_account_or_service_id_restriction", true)
                .get("/v1/agreement/a-valid-agreement-id")
                .then()
                .statusCode(200)
                .body("external_id", is("a-valid-agreement-id"))
                .body("status", is("ACTIVE"));
    }

    @Test
    public void shouldHandlePaymentInstrumentEvent() throws InterruptedException {
        var agreementId = "a-valid-agreement-id";
        aQueuePaymentEventFixture()
                .withResourceExternalId(agreementId)
                .withResourceType(ResourceType.AGREEMENT)
                .withEventDate(CREATED_AT)
                .withEventType("AGREEMENT_CREATED")
                .withLive(false)
                .withEventData(
                        gsonBuilder.create()
                                .toJson(Map.of(
                                        "reference", "agreement-reference",
                                        "description", "agreement description text",
                                        "status", "ACTIVE"
                                ))
                )
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId("a-valid-instrument-id")
                .withResourceType(ResourceType.PAYMENT_INSTRUMENT)
                .withLive(false)
                .withEventDate(CREATED_AT)
                .withEventType("PAYMENT_INSTRUMENT_CREATED")
                .withEventData(
                        gsonBuilder.create()
                                .toJson(Map.of(
                                        "cardholder_name", "A paying user name",
                                            "address_line1", "10 some street",
                                            "address_line2", "Some town",
                                            "address_postcode", "EC3R8BT",
                                            "address_city", "London",
                                            "address_country", "UK",
                                            "last_digits_card_number", "4242",
                                            "card_brand", "visa",
                                            "agreement_external_id", agreementId
                                ))
                )
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .queryParam("override_account_or_service_id_restriction", true)
                .get("/v1/agreement/" + agreementId)
                .then()
                .statusCode(200)
                .body("external_id", is(agreementId))
                .body("payment_instrument.external_id", is("a-valid-instrument-id"))
                .body("payment_instrument.agreement_external_id", is(agreementId))
                .body("payment_instrument.card_details.billing_address.line1", is("10 some street"))
                .body("payment_instrument.card_details.billing_address.postcode", is("EC3R8BT"))
                .body("payment_instrument.card_details.card_brand", is("visa"))
                .body("payment_instrument.card_details.last_digits_card_number", is("4242"))
                .body("payment_instrument.card_details.cardholder_name", is("A paying user name"));
    }
}