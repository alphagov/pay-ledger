package uk.gov.pay.ledger.transaction.resource;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.model.Payment;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.fixture.EventFixture;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static uk.gov.pay.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aPersistedTransactionList;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class TransactionResourceIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private Integer port = rule.getAppRule().getLocalPort();

    private TransactionFixture transactionFixture;
    private EventFixture eventFixture;

    @Before
    public void setUp() {
        transactionFixture = aTransactionFixture();
        aDatabaseTestHelper(rule.getJdbi()).truncateAllData();
    }

    @Test
    public void shouldGetTransaction() {

        transactionFixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withDefaultTransactionDetails();
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("transaction_id", is(transactionFixture.getExternalId()))
                .body("card_details.cardholder_name", is(transactionFixture.getCardDetails().getCardHolderName()))
                .body("card_details.billing_address.line1", is(transactionFixture.getCardDetails().getBillingAddress().getAddressLine1()));
    }

    @Test
    public void shouldGetRefundTransaction() {
        var now = ZonedDateTime.parse("2019-07-31T14:52:07.073Z");
        var refundedBy = "some_user_id";

        transactionFixture = aTransactionFixture()
                .withTransactionType("REFUND")
                .withState(TransactionState.SUCCESS)
                .withCreatedDate(now)
                .withRefundedById(refundedBy)
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
                .body("refunded_by", is(refundedBy));
    }

    @Test
    public void shouldSearchUsingAllFieldsAndReturnAllFieldsCorrectly() {
        String gatewayAccountId = RandomStringUtils.randomAlphanumeric(20);
        List<Transaction> transactionList = aPersistedTransactionList(gatewayAccountId, 20, rule.getJdbi(), true);
        Payment transactionToVerify = (Payment) transactionList.get(15);
        given().port(port)
                .contentType(JSON)
                //todo: add more query params (refund_states, payment_states...) when search functionality is available
                .get("/v1/transaction?" +
                        "account_id=" + gatewayAccountId +
                        "&page=2" +
                        "&display_size=2" +
                        "&email=example.org" +
                        "&reference=reference" +
                        "&cardholder_name=smith" +
                        "&from_date=2000-01-01T10:15:30Z" +
                        "&to_date=2100-01-01T10:15:30Z" +
                        "&state=submitted" +
                        "&card_brands=visa,mastercard" +
                        "&last_digits_card_number=1234" +
                        "&first_digits_card_number=123456" +
                        "&transaction_type=payment"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(2))
                .body("results[0].gateway_account_id", is(transactionToVerify.getGatewayAccountId()))
                .body("results[0].amount", is(transactionToVerify.getAmount().intValue()))
                .body("results[0].state.finished", is(false))
                .body("results[0].state.status", is(transactionToVerify.getState().getStatus()))
                .body("results[0].description", is(transactionToVerify.getDescription()))
                .body("results[0].reference", is(transactionToVerify.getReference()))
                .body("results[0].language", is(transactionToVerify.getLanguage()))
                .body("results[0].return_url", is(transactionToVerify.getReturnUrl()))
                .body("results[0].email", is(transactionToVerify.getEmail()))
                .body("results[0].created_date", is(ISO_INSTANT_MILLISECOND_PRECISION.format(transactionToVerify.getCreatedDate())))
                .body("results[0].payment_provider", is(transactionToVerify.getPaymentProvider()))
                .body("results[0].card_details.cardholder_name", is(transactionToVerify.getCardDetails().getCardHolderName()))
                .body("results[0].card_details.billing_address.line1", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressLine1()))
                .body("results[0].card_details.billing_address.line2", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressLine2()))
                .body("results[0].card_details.billing_address.postcode", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressPostCode()))
                .body("results[0].card_details.billing_address.city", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressCity()))
                .body("results[0].card_details.billing_address.country", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressCountry()))
                .body("results[0].card_details.card_brand", is(transactionToVerify.getCardDetails().getCardBrand()))
                .body("results[0].delayed_capture", is(transactionToVerify.getDelayedCapture()))
                .body("results[0].transaction_id", is(transactionToVerify.getExternalId()))

                .body("count", is(2))
                .body("page", is(2))
                .body("total", is(10))

                .body("_links.self.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&cardholder_name=smith&first_digits_card_number=123456&last_digits_card_number=1234&card_brand=visa%2Cmastercard&state=submitted&transaction_type=PAYMENT&page=2&display_size=2"))
                .body("_links.first_page.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&cardholder_name=smith&first_digits_card_number=123456&last_digits_card_number=1234&card_brand=visa%2Cmastercard&state=submitted&transaction_type=PAYMENT&page=1&display_size=2"))
                .body("_links.last_page.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&cardholder_name=smith&first_digits_card_number=123456&last_digits_card_number=1234&card_brand=visa%2Cmastercard&state=submitted&transaction_type=PAYMENT&page=5&display_size=2"))
                .body("_links.prev_page.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&cardholder_name=smith&first_digits_card_number=123456&last_digits_card_number=1234&card_brand=visa%2Cmastercard&state=submitted&transaction_type=PAYMENT&page=1&display_size=2"))
                .body("_links.next_page.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&cardholder_name=smith&first_digits_card_number=123456&last_digits_card_number=1234&card_brand=visa%2Cmastercard&state=submitted&transaction_type=PAYMENT&page=3&display_size=2"));
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
                .body("events[0].data", is(event.getEventData()));
    }

    @Test
    public void shouldReturnBadRequestStatusIfNoTransactionEventsFound() {
        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/some-external-id/event?gateway_account_id=some-gateway-id")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .contentType(JSON)
                .body("message", is("Transaction with id [some-external-id] not found"));
    }
}
