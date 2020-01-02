package uk.gov.pay.ledger.report.resource;

import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.commons.model.ErrorIdentifier;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class PerformanceReportResourceIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private Integer port = rule.getAppRule().getLocalPort();

    @Test
    public void report_volume_total_amount_and_average_amount() {
        LongStream.of(1200L, 1020L, 750L).forEach(amount -> aTransactionFixture()
                .withAmount(amount)
                .withState(TransactionState.SUCCESS)
                .withTransactionType("PAYMENT")
                .withLive(true)
                .insert(rule.getJdbi()));

        given().port(port)
                .contentType(JSON)
                .get("/v1/report/performance-report")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON).log().body()
                .body("total_volume", is(3))
                .body("total_amount", is(2970))
                .body("average_amount", is(990.0f));
    }

    @Test
    public void report_volume_total_amount_and_average_amount_for_date_range() {
        Stream.of("2019-12-12T10:00:00Z", "2019-12-11T10:00:00Z", "2017-11-30T10:00:00Z").forEach(time -> aTransactionFixture()
                .withCreatedDate(ZonedDateTime.parse(time))
                .withAmount(1000L)
                .withState(TransactionState.SUCCESS)
                .withTransactionType("PAYMENT")
                .withLive(true)
                .insert(rule.getJdbi()));

        given().port(port)
                .contentType(JSON)
                .queryParam("from_date", "2017-11-30T10:00:00Z")
                .queryParam("to_date", "2019-12-12T10:00:00Z")
                .get("/v1/report/performance-report")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON).log().body()
                .body("total_volume", is(3))
                .body("total_amount", is(3000))
                .body("average_amount", is(1000.0f));
    }

    @Test
    public void should_return_422_when_only_one_date_is_provided() {
        Stream.of("from_date", "to_date").forEach(queryParam ->
                given().port(port)
                        .contentType(JSON)
                        .queryParam(queryParam, "2017-11-30T10:00:00Z")
                        .get("/v1/report/performance-report")
                        .then()
                        .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .body("message", contains("Both from_date and to_date must be provided"))
                        .body("error_identifier", Matchers.is(ErrorIdentifier.GENERIC.toString())));
    }
}
