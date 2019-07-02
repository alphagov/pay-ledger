package uk.gov.pay.ledger.healthcheck;

import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.rule.SqsTestDocker;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class HealthCheckResourceIT {

    @Rule
    public AppWithPostgresAndSqsRule app = new AppWithPostgresAndSqsRule();

    @Test
    public void healthcheckIdentifiesHealthySQSQueue() {
        given().port(app.getAppRule().getLocalPort())
                .contentType(JSON)
                .when()
                .accept(JSON)
                .get("healthcheck")
                .then()
                .statusCode(200)
                .body("deadlocks.healthy", equalTo(true))
                .body("sqsQueue.healthy", equalTo(true));
    }

    @Test
    public void healthcheckIdentifiesConnectionFailureSQSQueue() {
        SqsTestDocker.stopContainer();
        given().port(app.getAppRule().getLocalPort())
                .contentType(JSON)
                .when()
                .accept(JSON)
                .get("healthcheck")
                .then()
                .statusCode(503)
                .body("deadlocks.healthy", equalTo(true))
                .body("sqsQueue.healthy", equalTo(false))
                .body("sqsQueue.message", containsString("Failed to retrieve queue attributes"));
    }
}