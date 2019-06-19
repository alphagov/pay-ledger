package uk.gov.pay.ledger.transaction.resource;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import javax.ws.rs.core.Response;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.pay.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aPersistedTransactionList;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class TransactionResourceIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private Integer port = rule.getAppRule().getLocalPort();

    private TransactionFixture transactionFixture;

    @Before
    public void setUp() {
        transactionFixture = aTransactionFixture();
    }

    @Test
    public void shouldGetTransaction() {

        transactionFixture = aTransactionFixture();
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("external_id", is(transactionFixture.getExternalId()))
                .body("card_details.cardholder_name", is(transactionFixture.getCardDetails().getCardHolderName()))
                .body("card_details.billing_address.line1", is(transactionFixture.getCardDetails().getBillingAddress().getAddressLine1()));
    }

    @Test
    public void shouldSearchUsingAllFieldsAndReturnAllFieldsCorrectly() {
        // until we use the date from the event we're sorting by id, that's why the list is inverted
        String gatewayAccountId = RandomStringUtils.randomAlphanumeric(20);
        List<Transaction> transactionList = aPersistedTransactionList(gatewayAccountId, 10, rule.getJdbi());
        Transaction transactionToVerify = transactionList.get(7);
        given().port(port)
                .contentType(JSON)
                //todo: add more query params (card_brands, refund_states...) when search functionality is available
                .get("/v1/transaction?" +
                        "account_id=" + gatewayAccountId +
                        "&page=2" +
                        "&display_size=2" +
                        "&email=example.org" +
                        "&reference=reference" +
                        "&card_holder_name=Smith" +
                        "&from_date=2000-01-01T10:15:30Z" +
                        "&to_date=2100-01-01T10:15:30Z" +
                        "&state=created"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("results[0].gateway_account_id", is(transactionToVerify.getGatewayAccountId()))
                .body("results[0].amount", is(transactionToVerify.getAmount().intValue()))
                .body("results[0].state.finished", is(false))
                .body("results[0].state.status", is(transactionToVerify.getState()))
                .body("results[0].description", is(transactionToVerify.getDescription()))
                .body("results[0].reference", is(transactionToVerify.getReference()))
                .body("results[0].language", is(transactionToVerify.getLanguage()))
                .body("results[0].return_url", is(transactionToVerify.getReturnUrl()))
                .body("results[0].email", is(transactionToVerify.getEmail()))
                .body("results[0].created_date", is(ISO_INSTANT_MILLISECOND_PRECISION.format(transactionToVerify.getCreatedAt())))
                .body("results[0].payment_provider", is(transactionToVerify.getPaymentProvider()))
                .body("results[0].card_details.cardholder_name", is(transactionToVerify.getCardDetails().getCardHolderName()))
                .body("results[0].card_details.billing_address.line1", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressLine1()))
                .body("results[0].card_details.billing_address.line2", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressLine2()))
                .body("results[0].card_details.billing_address.postcode", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressPostCode()))
                .body("results[0].card_details.billing_address.city", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressCity()))
                .body("results[0].card_details.billing_address.country", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressCountry()))
                .body("results[0].card_details.card_brand", is(transactionToVerify.getCardDetails().getCardBrand()))
                .body("results[0].delayed_capture", is(transactionToVerify.getDelayedCapture()))
                .body("results[0].charge_id", is(transactionToVerify.getExternalId()))
                .body("results[0].links[0].href", containsString("v1/transaction/" + transactionToVerify.getExternalId()))
                .body("results[0].links[0].method", is("GET"))
                .body("results[0].links[0].rel", is("self"))
                .body("results[0].links[1].href", containsString("v1/transaction/" + transactionToVerify.getExternalId() + "/refunds"))
                .body("results[0].links[1].method", is("GET"))
                .body("results[0].links[1].rel", is("refunds"))

                .body("count", is(2))
                .body("page", is(2))
                .body("total", is(10))

                .body("_links.self.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&page=2&display_size=2"))
                .body("_links.first_page.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&page=1&display_size=2"))
                .body("_links.last_page.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&page=5&display_size=2"))
                .body("_links.prev_page.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&page=1&display_size=2"))
                .body("_links.next_page.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&page=3&display_size=2"));
    }
}
