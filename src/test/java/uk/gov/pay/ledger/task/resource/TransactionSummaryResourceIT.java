package uk.gov.pay.ledger.task.resource;

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

public class TransactionSummaryResourceIT {

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
                .withState(TransactionState.SUCCESS)
                .withGatewayAccountId(gatewayAccountId)
                .withCreatedDate(ZonedDateTime.parse("2019-10-01T10:00:00.000Z"))
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/tasks/update-transaction-summary")
                .then()
                .log().all()
                .statusCode(Response.Status.OK.getStatusCode());
    }
}