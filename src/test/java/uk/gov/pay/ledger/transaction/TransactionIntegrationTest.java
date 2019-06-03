package uk.gov.pay.ledger.transaction;

import io.restassured.specification.RequestSpecification;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.rules.AppWithPostgresRule;
import uk.gov.pay.ledger.utils.fixtures.TransactionFixture;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.pay.ledger.utils.fixtures.TransactionFixture.aTransactionFixture;

public class TransactionIntegrationTest {

    @ClassRule
    public static AppWithPostgresRule rule = new AppWithPostgresRule();

    private Client client = rule.getAppRule().client();
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

        givenSetup()
                .get("/v1/transaction/" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("", hasSize(1))
                .body("[0].charge_id", is(transactionFixture.getExternalId()))
                .body("[0].card_details.cardholder_name", is(transactionFixture.getCardDetails().getCardHolderName()))
                .body("[0].card_details.billing_address.line1", is(transactionFixture.getCardDetails().getBillingAddress().getAddressLine1()));
    }

    private RequestSpecification givenSetup() {
        return given().port(port)
                .contentType(JSON);
    }
}
