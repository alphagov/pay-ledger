package uk.gov.pay.ledger.transaction;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.pay.ledger.rules.AppWithPostgresRule;
import uk.gov.pay.ledger.utils.fixtures.TransactionFixture;

import javax.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.pay.ledger.utils.fixtures.TransactionFixture.aTransactionFixture;

@Ignore
public class TransactionIntegrationTest {

    @ClassRule
    public static AppWithPostgresRule rule = new AppWithPostgresRule();

    private Integer port = rule.getAppRule().getLocalPort();

    private TransactionFixture transactionFixture;

    @Before
    public void setUp() {
        transactionFixture = aTransactionFixture();
    }

    @Test
    public void shouldGetTransactionsFromDB() {

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
}
