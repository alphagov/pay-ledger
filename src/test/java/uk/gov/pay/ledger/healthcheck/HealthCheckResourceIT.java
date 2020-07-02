package uk.gov.pay.ledger.healthcheck;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.rule.PostgresTestDocker;
import uk.gov.pay.ledger.rule.SqsTestDocker;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(DropwizardExtensionsSupport.class)
public class HealthCheckResourceIT {

    @RegisterExtension
    public AppWithPostgresAndSqsExtension app = new AppWithPostgresAndSqsExtension();

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
                .body("postgresql.healthy", equalTo(true))
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
                .body("postgresql.healthy", equalTo(true))
                .body("sqsQueue.healthy", equalTo(false))
                .body("sqsQueue.message", containsString("Failed to retrieve queue attributes"));
    }

    @Test
    public void healthcheckIdentifiesHealthyDatabase() {
        given().port(app.getAppRule().getLocalPort())
                .contentType(JSON)
                .when()
                .accept(JSON)
                .get("healthcheck")
                .then()
                .statusCode(200)
                .body("deadlocks.healthy", equalTo(true))
                .body("sqsQueue.healthy", equalTo(true))
                .body("postgresql.healthy", equalTo(true));
    }

    @Test
    public void healthcheckIdentifiesBadConnectionToDatabase() {
        PostgresTestDocker.stopContainer();
        given().port(app.getAppRule().getLocalPort())
                .contentType(JSON)
                .when()
                .accept(JSON)
                .get("healthcheck")
                .then()
                .statusCode(503)
                .body("deadlocks.healthy", equalTo(true))
                .body("sqsQueue.healthy", equalTo(true))
                .body("postgresql.healthy", equalTo(false))
                .body("postgresql.message", containsString("Unable to successfully check"));
    }
}