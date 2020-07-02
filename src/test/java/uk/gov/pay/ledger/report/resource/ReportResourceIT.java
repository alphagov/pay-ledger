package uk.gov.pay.ledger.report.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class ReportResourceIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private Integer port = rule.getAppRule().getLocalPort();
    private final String gatewayAccountId = "abc123";

    @BeforeEach
    public void setUp() {
        aDatabaseTestHelper(rule.getJdbi()).truncateAllData();
    }

    @Test
    public void shouldGetPaymentCountsByStatus() {
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
    public void shouldGetTransactionSummaryStatisticsForToolbox() {
        aTransactionFixture()
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withGatewayAccountId(gatewayAccountId)
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T10:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withAmount(2000L)
                .withState(TransactionState.SUCCESS)
                .withGatewayAccountId(gatewayAccountId)
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T10:00:00.000Z"))
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/report/transactions-summary?account_id=" + gatewayAccountId +
                        "&from_date=2019-10-01T09:00:00.000Z" +
                        "&to_date=2019-10-01T11:00:00.000Z" +
                        "&override_from_date_validation=true"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON).log().body()
                .body("payments.count", is(2))
                .body("payments.gross_amount", is(3000))
                .body("net_income", is(3000));
    }

    @Test
    public void shouldGetTransactionSummaryStatisticsWithMotoForToolbox() {
        aTransactionFixture()
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withGatewayAccountId(gatewayAccountId)
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T10:00:00.000Z"))
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withAmount(2000L)
                .withMoto(true)
                .withState(TransactionState.SUCCESS)
                .withGatewayAccountId(gatewayAccountId)
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T10:00:00.000Z"))
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/report/transactions-summary?account_id=" + gatewayAccountId +
                        "&include_moto_statistics=true" +
                        "&from_date=2019-10-01T09:00:00.000Z" +
                        "&to_date=2019-10-01T11:00:00.000Z" +
                        "&override_from_date_validation=true"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("payments.count", is(2))
                .body("payments.gross_amount", is(3000))
                .body("moto_payments.count", is(1))
                .body("moto_payments.gross_amount", is(2000))
                .body("net_income", is(3000));
    }

    @Test
    public void shouldGetTransactionSummaryForSelfservice() {
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withAmount(2000L)
                .withState(TransactionState.FAILED_REJECTED)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withAmount(4000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-29T00:00:00.000Z"))
                .withAmount(4000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId("another-gateway-account")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-29T00:00:00.000Z"))
                .withAmount(4000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.REFUND.name())
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T00:00:00.000Z"))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId("another-gateway-account")
                .withTransactionType(TransactionType.REFUND.name())
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T00:00:00.000Z"))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.REFUND.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-28T00:00:00.000Z"))
                .withAmount(3000L)
                .withState(TransactionState.SUCCESS)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType(TransactionType.REFUND.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T00:00:00.000Z"))
                .withAmount(2000L)
                .withState(TransactionState.FAILED_REJECTED)
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/report/transactions-summary?account_id=" + gatewayAccountId +
                        "&from_date=2019-09-29T23:59:59.000Z" +
                        "&to_date=2019-10-02T00:00:00.000Z"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("payments.count", is(1))
                .body("payments.gross_amount", is(4000))
                .body("refunds.count", is(1))
                .body("refunds.gross_amount", is(1000))
                .body("net_income", is(3000));
    }

    @Test
    public void shouldGetTimeseriesReportForTransactionVolumesByHour() {
        aTransactionFixture()
                .withGatewayAccountId("100")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T08:10:00.100Z"))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withLive(true)
                .insert(rule.getJdbi())
                .toEntity();

        aTransactionFixture()
                .withGatewayAccountId("100")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T08:20:00.000Z"))
                .withAmount(1000L)
                .withState(TransactionState.SUBMITTED)
                .withLive(true)
                .insert(rule.getJdbi())
                .toEntity();

        aTransactionFixture()
                .withGatewayAccountId("100")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T09:00:00.000Z"))
                .withAmount(1000L)
                .withState(TransactionState.ERROR_GATEWAY)
                .withLive(true)
                .insert(rule.getJdbi())
                .toEntity();

        given().port(port)
                .contentType(JSON)
                .queryParam("from_date", "2019-09-30T00:00:00.000Z")
                .queryParam("to_date", "2019-09-30T23:59:59.999Z")
                .get("/v1/report/transactions-by-hour")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("size()", is(2))
                .body("[0].all_payments", is(2))
                .body("[0].completed_payments", is(1))
                .body("[1].all_payments", is(1))
                .body("[1].errored_payments", is(1));
    }

    @Test
    public void shouldRejectTimeseriesReportForTransactionVolumesByHourWithInvalidParams() {
        aTransactionFixture()
                .withGatewayAccountId("100")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withCreatedDate(ZonedDateTime.parse("2019-09-30T08:10:00.100Z"))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withLive(true)
                .insert(rule.getJdbi())
                .toEntity();

        given().port(port)
                .contentType(JSON)
                .queryParam("to_date", "2019-09-30T23:59:59.999Z")
                .get("/v1/report/transactions-by-hour")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }
}