package uk.gov.pay.ledger.transaction.resource;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.commons.model.Source;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.fixture.EventFixture;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.pay.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class TransactionResourceIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private Integer port = rule.getAppRule().getLocalPort();

    private TransactionFixture transactionFixture;

    @Before
    public void setUp() {
        aDatabaseTestHelper(rule.getJdbi()).truncateAllData();
    }

    @Test
    public void shouldGetTransaction() {

        transactionFixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withDefaultTransactionDetails()
                .withLive(Boolean.TRUE)
                .withSource(String.valueOf(Source.CARD_API));
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("transaction_id", is(transactionFixture.getExternalId()))
                .body("card_details.cardholder_name", is(transactionFixture.getCardDetails().getCardHolderName()))
                .body("card_details.expiry_date", is(transactionFixture.getCardDetails().getExpiryDate()))
                .body("card_details.card_type", is(transactionFixture.getCardDetails().getCardType().toString().toLowerCase()))
                .body("card_details.billing_address.line1", is(transactionFixture.getCardDetails().getBillingAddress().getAddressLine1()))
                .body("live", is(Boolean.TRUE))
                .body("source", is(String.valueOf(Source.CARD_API)));
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

        transactionFixture = aTransactionFixture()
                .withTransactionType("REFUND")
                .withState(TransactionState.SUCCESS)
                .withCreatedDate(now)
                .withRefundedById(refundedBy)
                .withRefundedByUserEmail(refundedByUserEmail)
                .withDefaultTransactionDetails();
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("transaction_id", is(transactionFixture.getExternalId()))
                .body("state.status", is(transactionFixture.getState().getStatus()))
                .body("amount", is(transactionFixture.getAmount().intValue()))
                .body("reference", is(transactionFixture.getReference()))
                .body("created_date", is(now.toString()))
                .body("refunded_by", is(refundedBy))
                .body("refunded_by_user_email", is(refundedByUserEmail));
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
                .withGatewayAccountId(parentTransactionEntity.getGatewayAccountId())
                .withTransactionType("REFUND")
                .withState(TransactionState.SUCCESS)
                .withAmount(1000L)
                .withRefundedById("refund-by-user-id")
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
                .body("transactions[0].reference", is(refundTransactionEntity.getReference()))
                .body("transactions[0].created_date", is(ISO_INSTANT_MILLISECOND_PRECISION.format(refundTransactionEntity.getCreatedDate())))
                .body("transactions[0].refunded_by", is("refund-by-user-id"))
                .body("transactions[0].transaction_type", is(refundTransactionEntity.getTransactionType()))
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
}
