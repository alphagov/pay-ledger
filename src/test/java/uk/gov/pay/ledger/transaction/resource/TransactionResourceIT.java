package uk.gov.pay.ledger.transaction.resource;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.pay.ledger.transaction.model.Exemption3ds.EXEMPTION_HONOURED;
import static uk.gov.pay.ledger.transaction.model.Exemption3ds.EXEMPTION_OUT_OF_SCOPE;
import static uk.gov.pay.ledger.transaction.model.Exemption3ds.EXEMPTION_REJECTED;
import static uk.gov.pay.ledger.transaction.model.TransactionType.DISPUTE;
import static uk.gov.pay.ledger.transaction.model.TransactionType.REFUND;
import static uk.gov.pay.ledger.transaction.service.TransactionService.REDACTED_REFERENCE_NUMBER;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;
import static uk.gov.pay.ledger.util.fixture.PayoutFixture.PayoutFixtureBuilder.aPayoutFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;
import static uk.gov.service.payments.commons.model.CommonDateTimeFormatters.ISO_INSTANT_MILLISECOND_PRECISION;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.google.gson.JsonObject;

import uk.gov.pay.ledger.event.entity.EventEntity;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.Exemption3ds;
import uk.gov.pay.ledger.transaction.model.Exemption3dsRequested;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.EventFixture;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.Source;

public class TransactionResourceIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private Integer port = rule.getAppRule().getLocalPort();

    private TransactionFixture transactionFixture;

    private DatabaseTestHelper databaseTestHelper;

    @BeforeEach
    public void setUp() {
        databaseTestHelper = aDatabaseTestHelper(rule.getJdbi());
        databaseTestHelper.truncateAllData();
    }

    public TransactionResourceIT() {
        super();
    }

    @Test
    public void shouldRedactReference() {
        transactionFixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withReference("4242424242424242");
        transactionFixture.insert(rule.getJdbi());
        String eventData = "{\"address\": \"Silicon Valley\", \"reference\": \"4242424242424242\"}";
        anEventFixture().withEventData(eventData)
                .withEventType("PAYMENT_CREATED")
                .withResourceExternalId(transactionFixture.getExternalId())
                .insert(rule.getJdbi());
        anEventFixture().withEventData(eventData)
                .withEventType("PAYMENT_SUCCEEDED")
                .withResourceExternalId(transactionFixture.getExternalId())
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .post("/v1/transaction/redact-reference/" + transactionFixture.getExternalId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("reference", is(REDACTED_REFERENCE_NUMBER));

        var results = databaseTestHelper.getEventsByExternalId(transactionFixture.getExternalId());
        assertThat(results, hasSize(2));
        results.forEach(result ->
                assertThat(result.get("event_data").toString(), containsString(format("\"reference\": \"%s\"", REDACTED_REFERENCE_NUMBER))));
    }

    @Test
    public void shouldGetTransaction() {
        var gatewayPayoutId = "payout-id";

        transactionFixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withLive(Boolean.TRUE)
                .withSource(String.valueOf(Source.CARD_API))
                .withGatewayPayoutId(gatewayPayoutId)
                .withAgreementId("an-agreement-id")
                .withDisputed(true)
                .withDefaultTransactionDetails();
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("transaction_id", is(transactionFixture.getExternalId()))
                .body("service_id", is(transactionFixture.getServiceId()))
                .body("credential_external_id", is(transactionFixture.getCredentialExternalId()))
                .body("card_details.cardholder_name", is(transactionFixture.getCardDetails().getCardHolderName()))
                .body("card_details.expiry_date", is(transactionFixture.getCardDetails().getExpiryDate()))
                .body("card_details.card_type", is(transactionFixture.getCardDetails().getCardType().toString().toLowerCase()))
                .body("card_details.billing_address.line1", is(transactionFixture.getCardDetails().getBillingAddress().getAddressLine1()))
                .body("exemption", nullValue())
                .body("live", is(Boolean.TRUE))
                .body("wallet_type", is("APPLE_PAY"))
                .body("source", is(String.valueOf(Source.CARD_API)))
                .body("gateway_payout_id", is(gatewayPayoutId))
                .body("agreement_id", is("an-agreement-id"))
                .body("authorisation_mode", is("web"))
                .body("disputed", is(Boolean.TRUE));
    }

    @Test
    public void shouldGetTransactionWith3ds() {
        JsonObject transactionDetails = new JsonObject();
        transactionDetails.addProperty("requires_3ds", true);
        transactionDetails.addProperty("version_3ds", "2.1.0");

        transactionFixture = aTransactionFixture()
                .withDefaultTransactionDetails()
                .withTransactionDetails(transactionDetails.toString());
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("transaction_id", is(transactionFixture.getExternalId()))
                .body("authorisation_summary.three_d_secure.required", is(true))
                .body("authorisation_summary.three_d_secure.version", is("2.1.0"));
    }

    @Test
    public void shouldReturn404ForNonExistentTransaction() {
        transactionFixture = aTransactionFixture();
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/does-not-exist?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void shouldReturnTransactionSearchResponseObjectForNonExistentTransactionByReference() {
        transactionFixture = aTransactionFixture()
                .withReference("existing");
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction" +
                        "?reference=not-existing" +
                        "&account_id=" + transactionFixture.getGatewayAccountId()
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("page", is(1))
                .body("total", is(0))
                .body("count", is(0))
                .body("results.size()", is(0));
    }

    @ParameterizedTest
    @CsvSource({"?, %3F",
            "{ , %7B",
            "[ , %5B",
            "f{o{o}{ , f%7Bo%7Bo%7D%7B",
            "foo&, foo%26",
            "foo@ , foo%40",
            "foo@@ , foo%40%40",
            "foo=bar&baz=quux , foo%3Dbar%26baz%3Dquux",
            "foo bar , foo+bar",
            "% , %25",
            "%7B, %257B"
    })
    public void shouldReturnTransactionSearchResponseObjectForExistingTransactionWhenReferenceIsUrlEncoded(String rawValue, String encodedValue) {
        transactionFixture = aTransactionFixture()
                .withReference(rawValue);
        transactionFixture.insert(rule.getJdbi());
        
        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .urlEncodingEnabled(false)
                .get("/v1/transaction" +
                        "?reference=" +
                        encodedValue +
                        "&account_id=" + transactionFixture.getGatewayAccountId()
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("results[0].reference", is(rawValue));
    }
    
    @Test
    public void shouldReturnATransactionSearchResponseObjectBetweenTwoDates() {
        var fromDate = ZonedDateTime.parse("2023-11-01T00:00:00.000Z");
        var toDate = ZonedDateTime.parse("2023-12-01T00:00:00.000Z");
        var dateBetween = ZonedDateTime.parse("2023-11-15T14:52:07.073Z");

        transactionFixture = aTransactionFixture()
                .withGatewayAccountId("1")
                .withCreatedDate(dateBetween)
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?account_id=1&from_date=" + 
                        fromDate + 
                        "&to_date=" + 
                        toDate + 
                        "&page=1&display_size=100"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("results[0].created_date", is(dateBetween.toString()));
    }
    
    @Test
    public void shouldReturnAllTransactionSearchResponseObjectsForMultipleAccountIds() {
        for (int i = 1; i <= 3; i++) {
            transactionFixture = aTransactionFixture()
                    .withGatewayAccountId(Integer.toString(i));
            transactionFixture.insert(rule.getJdbi());
        }
        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?account_id=1,2,3")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("results[0].gateway_account_id", is("3"))
                .body("results[1].gateway_account_id", is("2"))
                .body("results[2].gateway_account_id", is("1"));
    }

    @Test
    public void shouldGetAllTransactionsForAmbiguousExternalState() {

        TransactionFixture cancelledTransaction1 = aTransactionFixture()
                .withDefaultCardDetails()
                .withGatewayAccountId("123")
                .withDefaultTransactionDetails()
                .withState(TransactionState.FAILED_CANCELLED)
                .insert(rule.getJdbi());
        TransactionFixture cancelledTransaction2 = aTransactionFixture()
                .withGatewayAccountId("123")
                .withDefaultCardDetails()
                .withDefaultTransactionDetails()
                .withState(TransactionState.CANCELLED)
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction" +
                        "?account_id=123" +
                        "&state=cancelled"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("page", is(1))
                .body("results[0].transaction_id", is(cancelledTransaction2.getExternalId()))
                .body("results[1].transaction_id", is(cancelledTransaction1.getExternalId()));
    }

    @Test
    public void shouldGetRefundTransaction() {
        var now = ZonedDateTime.parse("2019-07-31T14:52:07.073Z");
        var refundedBy = "some_user_id";
        var refundedByUserEmail = "test@example.com";
        var gatewayPayoutId = "payout-id" + randomAlphanumeric(10);

        transactionFixture = aTransactionFixture()
                .withTransactionType("REFUND")
                .withState(TransactionState.SUCCESS)
                .withCreatedDate(now)
                .withRefundedById(refundedBy)
                .withRefundedByUserEmail(refundedByUserEmail)
                .withGatewayTransactionId("gateway-transaction-id")
                .withCaptureSubmittedDate(now)
                .withCapturedDate(now)
                .withDefaultPaymentDetails()
                .withDefaultTransactionDetails()
                .withGatewayPayoutId(gatewayPayoutId);
        transactionFixture.insert(rule.getJdbi());

        aPayoutFixture()
                .withGatewayAccountId(transactionFixture.getGatewayAccountId())
                .withGatewayPayoutId(gatewayPayoutId)
                .withPaidOutDate(now)
                .build()
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("transaction_id", is(transactionFixture.getExternalId()))
                .body("service_id", is(transactionFixture.getServiceId()))
                .body("live", is(transactionFixture.isLive()))
                .body("state.status", is(transactionFixture.getState().getStatus()))
                .body("amount", is(transactionFixture.getAmount().intValue()))
                .body("gateway_transaction_id", is(transactionFixture.getGatewayTransactionId()))
                .body("created_date", is(now.toString()))
                .body("refunded_by", is(refundedBy))
                .body("refunded_by_user_email", is(refundedByUserEmail))
                .body("gateway_payout_id", is(gatewayPayoutId))
                .body("payment_details.description", is(transactionFixture.getDescription()))
                .body("payment_details.reference", is(transactionFixture.getReference()))
                .body("payment_details.email", is(transactionFixture.getEmail()))
                .body("payment_details.transaction_type", is("PAYMENT"))
                .body("payment_details.card_details.cardholder_name", is(transactionFixture.getCardholderName()))
                .body("payment_details.card_details.card_brand", is(transactionFixture.getCardBrandLabel()))
                .body("payment_details.card_details.last_digits_card_number", is(transactionFixture.getLastDigitsCardNumber()))
                .body("payment_details.card_details.first_digits_card_number", is(transactionFixture.getFirstDigitsCardNumber()))
                .body("payment_details.card_details.expiry_date", is(transactionFixture.getCardExpiryDate()))
                .body("payment_details.card_details.card_type", is("credit"))
                .body("$", not(hasKey("captured_submit_time")))
                .body("$", not(hasKey("captured_date")))
                .body("settlement_summary.settled_date", is(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
    }

    @Test
    public void shouldReturnTransactionEventsCorrectly() {
        transactionFixture = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withDefaultCardDetails()
                .withDefaultTransactionDetails();
        transactionFixture.insert(rule.getJdbi());

        EventEntity event = EventFixture.anEventFixture()
                .withResourceExternalId(transactionFixture.getExternalId())
                .withEventDate(ZonedDateTime.parse("2019-07-31T09:52:43.451Z"))
                .insert(rule.getJdbi())
                .toEntity();

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "/event?gateway_account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("transaction_id", is(transactionFixture.getExternalId()))
                .body("events[0].amount", is(transactionFixture.getAmount().intValue()))
                .body("events[0].state.status", is("created"))
                .body("events[0].state.finished", is(false))
                .body("events[0].resource_type", is("PAYMENT"))
                .body("events[0].event_type", is(event.getEventType()))
                .body("events[0].timestamp", is("2019-07-31T09:52:43.451Z"))
                .body("events[0].data.event_data", is("event data"));
    }

    @Test
    public void shouldReturnBadRequestStatusIfNoTransactionEventsFound() {
        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/some-external-id/event?gateway_account_id=some-gateway-id")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode())
                .contentType(JSON)
                .body("message", is("Transaction with id [some-external-id] not found"));
    }

    @Test
    public void getTransactionsForTransactionShouldReturnTransactionsWithParentExternalId_notFilteredByTransactionType() {
        TransactionEntity parentTransactionEntity = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withDefaultCardDetails()
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity refundTransactionEntity = aTransactionFixture()
                .withParentExternalId(parentTransactionEntity.getExternalId())

                .withReference(parentTransactionEntity.getReference())
                .withDescription(parentTransactionEntity.getDescription())
                .withEmail(parentTransactionEntity.getEmail())
                .withCardholderName(parentTransactionEntity.getCardholderName())

                .withGatewayAccountId(parentTransactionEntity.getGatewayAccountId())
                .withTransactionType(REFUND.name())
                .withState(TransactionState.SUCCESS)
                .withAmount(1000L)
                .withRefundedById("refund-by-user-id")
                .withGatewayTransactionId("gateway-transaction-id")
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity disputeTransactionEntity = aTransactionFixture()
                .withParentExternalId(parentTransactionEntity.getExternalId())
                .withGatewayAccountId(parentTransactionEntity.getGatewayAccountId())
                .withTransactionType(DISPUTE.name())
                .insert(rule.getJdbi())
                .toEntity();

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + parentTransactionEntity.getExternalId() + "/transaction?gateway_account_id=" + parentTransactionEntity.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("parent_transaction_id", is(parentTransactionEntity.getExternalId()))
                .body("transactions", hasSize(2))
                .body("transactions[0].gateway_account_id", is(refundTransactionEntity.getGatewayAccountId()))
                .body("transactions[0].amount", is(1000))
                .body("transactions[0].state.status", is(refundTransactionEntity.getState().getStatus()))
                .body("transactions[0].state.finished", is(true))
                .body("transactions[0].gateway_transaction_id", is(refundTransactionEntity.getGatewayTransactionId()))
                .body("transactions[0].created_date", is(ISO_INSTANT_MILLISECOND_PRECISION.format(refundTransactionEntity.getCreatedDate())))
                .body("transactions[0].refunded_by", is("refund-by-user-id"))
                .body("transactions[0].transaction_type", is(refundTransactionEntity.getTransactionType()))
                .body("transactions[0].transaction_id", is(refundTransactionEntity.getExternalId()))
                .body("transactions[0].payment_details.description", is(parentTransactionEntity.getDescription()))
                .body("transactions[0].payment_details.reference", is(parentTransactionEntity.getReference()))
                .body("transactions[0].payment_details.email", is(parentTransactionEntity.getEmail()))
                .body("transactions[0].payment_details.card_details.cardholder_name", is(parentTransactionEntity.getCardholderName()))
                .body("transactions[1].transaction_id", is(disputeTransactionEntity.getExternalId()));
    }

    @Test
    public void getTransactionsForTransactionShouldReturnTransactionsWithParentExternalId_filteredByTransactionType() {
        TransactionEntity parentTransactionEntity = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withDefaultCardDetails()
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity refundTransactionEntity = aTransactionFixture()
                .withParentExternalId(parentTransactionEntity.getExternalId())
                .withGatewayAccountId(parentTransactionEntity.getGatewayAccountId())
                .withTransactionType(REFUND.name())
                .insert(rule.getJdbi())
                .toEntity();
        aTransactionFixture()
                .withParentExternalId(parentTransactionEntity.getExternalId())
                .withGatewayAccountId(parentTransactionEntity.getGatewayAccountId())
                .withTransactionType(DISPUTE.name())
                .insert(rule.getJdbi())
                .toEntity();

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + parentTransactionEntity.getExternalId() + "/transaction?gateway_account_id=" + parentTransactionEntity.getGatewayAccountId() + "&transaction_type=REFUND")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("parent_transaction_id", is(parentTransactionEntity.getExternalId()))
                .body("transactions", hasSize(1))
                .body("transactions[0].transaction_id", is(refundTransactionEntity.getExternalId()));
    }

    @Test
    public void getTransactionsForTransactionShouldReturnEmptyListIfParentTransactionHasNoTransactions() {
        transactionFixture = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withGatewayAccountId("1")
                .withDefaultCardDetails()
                .withDefaultTransactionDetails();
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "/transaction?gateway_account_id=1")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("parent_transaction_id", is(transactionFixture.getExternalId()))
                .body("transactions", Matchers.hasSize(0));
    }

    @Test
    public void getTransactionsForTransactionShouldReturnHttpNotFoundIfParentTransactionDoesNotExist() {
        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/some-parent-external-id/transaction?gateway_account_id=1")
                .then()
                .contentType(JSON)
                .statusCode(Response.Status.NOT_FOUND.getStatusCode())
                .body("message", is("Transaction with id [some-parent-external-id] not found"));
    }

    @Test
    public void returnedTransactionsShould_haveCorrectSourceAndLive() {
        TransactionFixture cancelledTransaction1 = aTransactionFixture()
                .withDefaultCardDetails()
                .withGatewayAccountId("123")
                .withDefaultTransactionDetails()
                .withSource(String.valueOf(Source.CARD_API))
                .withLive(Boolean.TRUE)
                .withState(TransactionState.FAILED_CANCELLED)
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction" +
                        "?account_id=123" +
                        "&state=cancelled"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("page", is(1))
                .body("results[0].live", is(Boolean.TRUE))
                .body("results[0].source", is(String.valueOf(Source.CARD_API)));
    }

    @Test
    public void getByGatewayTransactionId_shouldReturnCorrectTransaction() {

        String gatewayTransactionIdParam = RandomStringUtils.randomAlphanumeric(20);
        String gatewayAccountId = RandomStringUtils.randomNumeric(5);

        TransactionFixture shouldExcludeThisTransaction = aTransactionFixture()
                .withGatewayAccountId(RandomStringUtils.randomNumeric(5))
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi());
        TransactionFixture shouldExcludeThisTransactionToo = aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayTransactionId("random-gateway-transaction-id")
                .withPaymentProvider("random-provider")
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi());
        TransactionFixture transaction = aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayTransactionId(gatewayTransactionIdParam)
                .withPaymentProvider("sandbox")
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction/gateway-transaction/" + gatewayTransactionIdParam +
                        "?payment_provider=sandbox"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("gateway_account_id", is(transaction.getGatewayAccountId()))
                .body("gateway_transaction_id", is(transaction.getGatewayTransactionId()))
                .body("payment_provider", is("sandbox"));
    }

    @Test
    public void shouldGetDisputeTransaction() {
        var createdDate = ZonedDateTime.parse("2022-06-08T11:22:48.822408Z");
        var paidOutDate = ZonedDateTime.parse("2022-07-08T12:20:07.073Z");
        ZonedDateTime evidenceDueDate = ZonedDateTime.parse("2022-05-10T22:59:59.000000Z");

        TransactionEntity parentTransactionEntity = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withDefaultCardDetails()
                .withDefaultTransactionDetails()
                .withGatewayAccountId("1")
                .withExternalId("blabla")
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity disputeTransactionEntity = aTransactionFixture()
                .withGatewayAccountId(parentTransactionEntity.getGatewayAccountId())
                .withParentExternalId(parentTransactionEntity.getExternalId())
                .withReference(parentTransactionEntity.getReference())
                .withDescription(parentTransactionEntity.getDescription())
                .withEmail(parentTransactionEntity.getEmail())
                .withCardholderName(parentTransactionEntity.getCardholderName())
                .withGatewayAccountId(parentTransactionEntity.getGatewayAccountId())
                .withTransactionType("DISPUTE")
                .withState(TransactionState.LOST)
                .withAmount(1000L)
                .withNetAmount(-2500L)
                .withGatewayTransactionId("gateway-transaction-id")
                .withTransactionDetails("{\"amount\": 1000, \"payment_details\": {\"card_type\": \"CREDIT\", \"expiry_date\": \"11/23\", \"card_brand_label\": \"Visa\"}, \"gateway_account_id\": \"1\", \"gateway_transaction_id\": \"du_dl20kdldj20ejs103jns\", \"reason\": \"fraudulent\", \"evidence_due_date\": \"" + evidenceDueDate + "\"}")
                .withEventCount(3)
                .withCardBrand(parentTransactionEntity.getCardBrand())
                .withFee(1500L)
                .withGatewayTransactionId("du_dl20kdldj20ejs103jns")
                .withServiceId(parentTransactionEntity.getServiceId())
                .withGatewayPayoutId("po_dl0e0sdlejskfklsele")
                .withCreatedDate(createdDate)
                .withLive(true)
                .insert(rule.getJdbi())
                .toEntity();

        aPayoutFixture()
                .withGatewayAccountId(parentTransactionEntity.getGatewayAccountId())
                .withGatewayPayoutId("po_dl0e0sdlejskfklsele")
                .withPaidOutDate(paidOutDate)
                .build()
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + disputeTransactionEntity.getExternalId() +
                        "?account_id=" + parentTransactionEntity.getGatewayAccountId() +
                        "&override_account_id_restriction=false" +
                        "transaction_type=DISPUTE" +
                        "parent_external_id=" + parentTransactionEntity.getExternalId() +
                        "status_version=2")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("gateway_account_id", is(disputeTransactionEntity.getGatewayAccountId()))
                .body("service_id", is(disputeTransactionEntity.getServiceId()))
                .body("amount", is(1000))
                .body("net_amount", is(-2500))
                .body("fee", is(1500))
                .body("state.finished", is(true))
                .body("state.status", is("lost"))
                .body("created_date", is(ISO_INSTANT_MILLISECOND_PRECISION.format(createdDate)))
                .body("gateway_transaction_id", is(disputeTransactionEntity.getGatewayTransactionId()))
                .body("evidence_due_date", is(ISO_INSTANT_MILLISECOND_PRECISION.format(evidenceDueDate)))
                .body("reason", is("fraudulent"))
                .body("settlement_summary.settled_date", is(paidOutDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                .body("transaction_type", is("DISPUTE"))
                .body("live", is(true))
                .body("parent_transaction_id", is(parentTransactionEntity.getExternalId()))
                .body("transaction_id", is(disputeTransactionEntity.getExternalId()))
                .body("payment_details.description", is(parentTransactionEntity.getDescription()))
                .body("payment_details.reference", is(parentTransactionEntity.getReference()))
                .body("payment_details.email", is(parentTransactionEntity.getEmail()))
                .body("payment_details.transaction_type", is("PAYMENT"))
                .body("payment_details.card_details.card_type", is("credit"));
    }

    @Test
    public void shouldGetFailedTransactionWithCanRetry() {
        transactionFixture = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.FAILED_REJECTED)
                .withCanRetry(false)
                .withAuthorisationMode(AuthorisationMode.AGREEMENT)
                .withDefaultCardDetails()
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("gateway_account_id", is(transactionFixture.getGatewayAccountId()))
                .body("gateway_transaction_id", is(transactionFixture.getGatewayTransactionId()))
                .body("state.can_retry", is(false))
                .body("authorisation_mode", is("agreement"));
    }

    @Test
    public void shouldGetTransactionsWithRightExternalState() {

        TransactionFixture cancelledTransaction = aTransactionFixture()
                .withDefaultCardDetails()
                .withGatewayAccountId("123")
                .withDefaultTransactionDetails()
                .withState(TransactionState.FAILED_CANCELLED)
                .insert(rule.getJdbi());
        TransactionFixture rejectedTransaction = aTransactionFixture()
                .withGatewayAccountId("123")
                .withState(TransactionState.FAILED_REJECTED)
                .withCanRetry(false)
                .withAuthorisationMode(AuthorisationMode.AGREEMENT)
                .withDefaultCardDetails()
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction" +
                        "?account_id=123"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("page", is(1))
                .body("results[0].transaction_id", is(rejectedTransaction.getExternalId()))
                .body("results[1].transaction_id", is(cancelledTransaction.getExternalId()))
                .body("results[0].state.can_retry", is(false))
                .body("results[1].state.can_retry", is(nullValue()));
    }

    private TransactionFixture createTransactionFixtureWithExemption(Exemption3ds exemption3ds, Exemption3dsRequested exemption3dsRequested) {
        var gatewayPayoutId = "payout-id";
        return aTransactionFixture()
                .withDefaultCardDetails()
                .withLive(Boolean.TRUE)
                .withSource(String.valueOf(Source.CARD_API))
                .withGatewayPayoutId(gatewayPayoutId)
                .withExemption3ds(exemption3ds)
                .withExemption3dsRequested(exemption3dsRequested)
                .withDefaultTransactionDetails();
    }

    @Test
    public void shouldGetTransactionWithNoExemptionInformation() {
        transactionFixture = createTransactionFixtureWithExemption(null, null);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption", nullValue());
    }

    @Test
    public void shouldGetTransactionWithExemptionNotRequested() {
        transactionFixture = createTransactionFixtureWithExemption(Exemption3ds.EXEMPTION_NOT_REQUESTED, null);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption.type", nullValue())
                .body("exemption.requested", is(Boolean.FALSE));
    }


    @Test
    public void shouldGetTransactionWithExemptionNotRequestedAndNoTypeEvenWithExemption3dsRequestedCorporate() {
        transactionFixture = createTransactionFixtureWithExemption(Exemption3ds.EXEMPTION_NOT_REQUESTED, Exemption3dsRequested.CORPORATE);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption.type", nullValue())
                .body("exemption.requested", is(Boolean.FALSE));
    }

    @Test
    public void shouldGetTransactionWithExemptionNotRequestedAndNoTypeEvenWithExemption3dsRequestedOptimised() {
        transactionFixture = createTransactionFixtureWithExemption(Exemption3ds.EXEMPTION_NOT_REQUESTED, Exemption3dsRequested.OPTIMISED);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption.type", nullValue())
                .body("exemption.requested", is(Boolean.FALSE));
    }

    @Test
    public void shouldGetTransactionWithExemptionRequestedButNoOutcomeYet() {
        transactionFixture = createTransactionFixtureWithExemption(null, Exemption3dsRequested.OPTIMISED);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption.requested", is(Boolean.TRUE));
    }

    @Test
    public void shouldGetTransactionWithExemptiondHonoured() {
        transactionFixture = createTransactionFixtureWithExemption(Exemption3ds.EXEMPTION_HONOURED, null);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption.requested", is(Boolean.TRUE))
                .body("exemption.outcome.result", is(EXEMPTION_HONOURED.toString()));
    }

    @Test
    public void shouldGetTransactionWithOptimisedExemptiondHonoured() {
        transactionFixture = createTransactionFixtureWithExemption(Exemption3ds.EXEMPTION_HONOURED, Exemption3dsRequested.OPTIMISED);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption.requested", is(Boolean.TRUE))
                .body("exemption.outcome.result", is(EXEMPTION_HONOURED.toString()));
    }

    @Test
    public void shouldGetTransactionWithExemptionRejected() {
        transactionFixture = createTransactionFixtureWithExemption(Exemption3ds.EXEMPTION_REJECTED, null);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption.requested", is(Boolean.TRUE))
                .body("exemption.outcome.result", is(EXEMPTION_REJECTED.toString()));
    }

    @Test
    public void shouldGetTransactionWithOptimisedExemptionRejected() {
        transactionFixture = createTransactionFixtureWithExemption(Exemption3ds.EXEMPTION_REJECTED, Exemption3dsRequested.OPTIMISED);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption.requested", is(Boolean.TRUE))
                .body("exemption.outcome.result", is(EXEMPTION_REJECTED.toString()));
    }

    @Test
    public void shouldGetTransactionWithExemptionOutOfScope() {
        transactionFixture = createTransactionFixtureWithExemption(Exemption3ds.EXEMPTION_OUT_OF_SCOPE, null);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption.requested", is(Boolean.TRUE))
                .body("exemption.outcome.result", is(EXEMPTION_OUT_OF_SCOPE.toString()));
    }


    @Test
    public void shouldGetTransactionWithOptimisedExemptionOutOfScope() {
        transactionFixture = createTransactionFixtureWithExemption(Exemption3ds.EXEMPTION_OUT_OF_SCOPE, Exemption3dsRequested.OPTIMISED);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption.requested", is(Boolean.TRUE))
                .body("exemption.outcome.result", is(EXEMPTION_OUT_OF_SCOPE.toString()));
    }

    @Test
    public void shouldGetTransactionWithCorporateExemptionRequested() {
        transactionFixture = createTransactionFixtureWithExemption(null, Exemption3dsRequested.CORPORATE);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption.requested", is(Boolean.TRUE))
                .body("exemption.type", is(Exemption3dsRequested.CORPORATE.toString()))
                .body("exemption.outcome", nullValue());
    }

    @Test
    public void shouldGetTransactionWithCorporateExemptionHonoured() {
        transactionFixture = createTransactionFixtureWithExemption(Exemption3ds.EXEMPTION_HONOURED, Exemption3dsRequested.CORPORATE);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption.requested", is(Boolean.TRUE))
                .body("exemption.type", is(Exemption3dsRequested.CORPORATE.toString()))
                .body("exemption.outcome.result", is(EXEMPTION_HONOURED.toString()));
    }

        @Test
    public void shouldGetTransactionWithCorporateExemptionRejected() {
        transactionFixture = createTransactionFixtureWithExemption(Exemption3ds.EXEMPTION_REJECTED, Exemption3dsRequested.CORPORATE);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption.requested", is(Boolean.TRUE))
                .body("exemption.type", is(Exemption3dsRequested.CORPORATE.toString()))
                .body("exemption.outcome.result", is(EXEMPTION_REJECTED.toString()));
    }

    @Test
    public void shouldGetTransactionWithCorporateExemptionOutOfScope() {
        transactionFixture = createTransactionFixtureWithExemption(Exemption3ds.EXEMPTION_OUT_OF_SCOPE, Exemption3dsRequested.CORPORATE);
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("exemption.requested", is(Boolean.TRUE))
                .body("exemption.type", is(Exemption3dsRequested.CORPORATE.toString()))
                .body("exemption.outcome.result", is(EXEMPTION_OUT_OF_SCOPE.toString()));
    }
}
