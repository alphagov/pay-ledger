package uk.gov.pay.ledger.report.resource;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.service.payments.commons.model.ErrorIdentifier;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class PerformanceReportResourceIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private Integer port = rule.getAppRule().getLocalPort();

    private DatabaseTestHelper databaseTestHelper = aDatabaseTestHelper(rule.getJdbi());

    @AfterEach
    public void tearDown() {
        databaseTestHelper.truncateAllData();
    }

    @Test
    public void should_return_400_when_invalid_state_provided() {
        given().port(port)
                .contentType(JSON)
                .queryParam("state", "wtf")
                .get("/v1/report/performance-report")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("message", contains("State provided must be one of uk.gov.pay.ledger.transaction.state.TransactionState"))
                .body("error_identifier", Matchers.is(ErrorIdentifier.GENERIC.toString()));
    }

    @Test
    public void report_volume_total_amount_and_average_amount_for_all_payments() {
        Stream.of(TransactionState.STARTED, TransactionState.SUCCESS, TransactionState.SUBMITTED).forEach(state ->
                aTransactionFixture()
                        .withAmount(1000L)
                        .withState(state)
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
                .body("total_amount", is(3000))
                .body("average_amount", is(1000.0f));
    }

    @Test
    public void report_volume_total_amount_and_average_amount_by_state() {
        aTransactionFixture()
                .withAmount(1000L)
                .withState(TransactionState.SUBMITTED)
                .withTransactionType("PAYMENT")
                .withLive(true)
                .insert(rule.getJdbi());

        LongStream.of(1200L, 1020L, 750L).forEach(amount -> aTransactionFixture()
                .withAmount(amount)
                .withState(TransactionState.SUCCESS)
                .withTransactionType("PAYMENT")
                .withLive(true)
                .insert(rule.getJdbi()));

        given().port(port)
                .contentType(JSON)
                .queryParam("state", TransactionState.SUCCESS.name())
                .get("/v1/report/performance-report")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
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
    public void should_return_400_when_only_one_date_is_provided() {
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

    @Test
    public void report_volume_total_amount_and_average_amount_for_date_range_per_gateway_account() {
        Stream.of("2019-12-12T10:00:00Z", "2019-12-11T10:00:00Z", "2017-11-30T10:00:00Z").forEach(time -> aTransactionFixture()
                .withCreatedDate(ZonedDateTime.parse(time))
                .withAmount(1000L)
                .withGatewayAccountId("1")
                .withState(TransactionState.SUCCESS)
                .withTransactionType("PAYMENT")
                .withLive(true)
                .insert(rule.getJdbi()));

        Stream.of("2019-12-12T10:00:00Z", "2019-12-11T10:00:00Z", "2017-11-30T10:00:00Z").forEach(time -> aTransactionFixture()
                .withCreatedDate(ZonedDateTime.parse(time))
                .withAmount(1000L)
                .withGatewayAccountId("2")
                .withState(TransactionState.SUCCESS)
                .withTransactionType("PAYMENT")
                .withLive(true)
                .insert(rule.getJdbi()));

        given().port(port)
                .contentType(JSON)
                .queryParam("from_date", "2017-11-30T10:00:00Z")
                .queryParam("to_date", "2019-12-11T10:00:00Z")
                .get("/v1/report/gateway-performance-report")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON).log().body()
                .body("[0].gateway_account_id", is(1))
                .body("[0].total_volume", is(1))
                .body("[0].total_amount", is(1000))
                .body("[0].average_amount", is(1000.0f))
                .body("[0].minimum_amount", is(1000))
                .body("[0].maximum_amount", is(1000))
                .body("[0].month", is(11))
                .body("[0].year", is(2017))
                .body("[1].gateway_account_id", is(1))
                .body("[1].total_volume", is(1))
                .body("[1].total_amount", is(1000))
                .body("[1].average_amount", is(1000.0f))
                .body("[1].minimum_amount", is(1000))
                .body("[1].maximum_amount", is(1000))
                .body("[1].month", is(12))
                .body("[1].year", is(2019))
                .body("[2].gateway_account_id", is(2))
                .body("[2].total_volume", is(1))
                .body("[2].total_amount", is(1000))
                .body("[2].average_amount", is(1000.0f))
                .body("[2].minimum_amount", is(1000))
                .body("[2].maximum_amount", is(1000))
                .body("[2].month", is(11))
                .body("[2].year", is(2017))
                .body("[3].gateway_account_id", is(2))
                .body("[3].total_volume", is(1))
                .body("[3].total_amount", is(1000))
                .body("[3].average_amount", is(1000.0f))
                .body("[3].minimum_amount", is(1000))
                .body("[3].maximum_amount", is(1000))
                .body("[3].month", is(12))
                .body("[3].year", is(2019));
    }

    @Test
    public void should_return_400_when_only_one_date_is_provided_for_gateway_performance_report() {
        Stream.of("from_date", "to_date").forEach(queryParam ->
                given().port(port)
                        .contentType(JSON)
                        .queryParam(queryParam, "2017-11-30T00:00:00Z")
                        .get("/v1/report/gateway-performance-report")
                        .then()
                        .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .body("message", contains("Both from_date and to_date must be provided"))
                        .body("error_identifier", Matchers.is(ErrorIdentifier.GENERIC.toString())));
    }

    @Test
    public void should_return_400_when_from_date_is_later_than_to_date_for_gateway_performance_report() {

        given().port(port)
                .contentType(JSON)
                .queryParam( "from_date", "2019-11-30T00:00:00Z")
                .queryParam( "to_date", "2017-11-30T00:00:00Z")
                .get("/v1/report/gateway-performance-report")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("message", contains("from_date must be earlier or equal to to_date"))
                .body("error_identifier", Matchers.is(ErrorIdentifier.GENERIC.toString()));
    }
}
