package uk.gov.pay.ledger.report.resource;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class ReportResourceIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private Integer port = rule.getAppRule().getLocalPort();

    @Before
    public void setUp() {
        aDatabaseTestHelper(rule.getJdbi()).truncateAllData();
    }

    @Test
    public void shouldGetPaymentCountsByStatus() {
        String gatewayAccountId = "abc123";
        aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withGatewayAccountId(gatewayAccountId)
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T10:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withState(TransactionState.SUBMITTED)
                .withGatewayAccountId(gatewayAccountId)
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T10:00:00.000Z"))
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/report/payments_by_state?account_id=" + gatewayAccountId +
                        "&from_date=2019-10-01T09:00:00.000Z" +
                        "&to_date=2019-10-01T11:00:00.000Z"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("undefined", is(0))
                .body("created", is(1))
                .body("started", is(0))
                .body("submitted", is(1))
                .body("capturable", is(0))
                .body("success", is(0))
                .body("declined", is(0))
                .body("timedout", is(0))
                .body("cancelled", is(0))
                .body("error", is(0));
    }

    @Test
    public void shouldGetPaymentsStatistics() {
        String gatewayAccountId = "abc123";
        aTransactionFixture()
                .withTotalAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withGatewayAccountId(gatewayAccountId)
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T10:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withTotalAmount(2000L)
                .withState(TransactionState.SUCCESS)
                .withGatewayAccountId(gatewayAccountId)
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T10:00:00.000Z"))
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/report/payments?account_id=" + gatewayAccountId +
                        "&from_date=2019-10-01T09:00:00.000Z" +
                        "&to_date=2019-10-01T11:00:00.000Z"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(2))
                .body("gross_amount", is(3000));
    }
}