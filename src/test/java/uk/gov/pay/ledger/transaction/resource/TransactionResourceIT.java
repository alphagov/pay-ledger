package uk.gov.pay.ledger.transaction.resource;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.fixture.EventFixture;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;
import uk.gov.service.payments.commons.model.Source;

import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.time.ZoneOffset.UTC;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasKey;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.PayoutFixture.PayoutFixtureBuilder.aPayoutFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;
import static uk.gov.service.payments.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;

public class TransactionResourceIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private Integer port = rule.getAppRule().getLocalPort();

    private TransactionFixture transactionFixture;

    @BeforeEach
    public void setUp() {
        aDatabaseTestHelper(rule.getJdbi()).truncateAllData();
    }

    @Test
    public void shouldGetTransaction() {
        var gatewayPayoutId = "payout-id";

        transactionFixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withDefaultTransactionDetails()
                .withLive(Boolean.TRUE)
                .withSource(String.valueOf(Source.CARD_API))
                .withGatewayPayoutId(gatewayPayoutId)
                .withAgreementId("an-agreement-id");
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
                .body("live", is(Boolean.TRUE))
                .body("wallet_type", is("APPLE_PAY"))
                .body("source", is(String.valueOf(Source.CARD_API)))
                .body("gateway_payout_id", is(gatewayPayoutId))
                .body("agreement_id", is("an-agreement-id"))
                .body("authorisation_mode", is("web"));
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

        Event event = EventFixture.anEventFixture()
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
    public void getTransactionsForTransactionShouldReturnTransactionsWithParentExternalId() {
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
                .withTransactionType("REFUND")
                .withState(TransactionState.SUCCESS)
                .withAmount(1000L)
                .withRefundedById("refund-by-user-id")
                .withGatewayTransactionId("gateway-transaction-id")
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi())
                .toEntity();

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + parentTransactionEntity.getExternalId() + "/transaction?gateway_account_id=" + parentTransactionEntity.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("parent_transaction_id", is(parentTransactionEntity.getExternalId()))
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
                .body("transactions[0].payment_details.card_details.cardholder_name", is(parentTransactionEntity.getCardholderName()));
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
    public void getTransactionsForTransactionShouldReturnHttpNotFoundIfParentTranscationDoesNotExist() {
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
}
