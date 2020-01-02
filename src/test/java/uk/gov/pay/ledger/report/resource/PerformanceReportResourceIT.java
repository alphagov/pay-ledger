package uk.gov.pay.ledger.report.resource;

import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import javax.ws.rs.core.Response;
import java.util.stream.LongStream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
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
}
