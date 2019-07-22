package uk.gov.pay.ledger.queue;

import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.event.model.EventType;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;

import java.time.ZonedDateTime;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.pay.ledger.util.fixture.QueueEventFixture.aQueueEventFixture;

public class QueueMessageReceiverIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule(config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true"));

    private static final ZonedDateTime CREATED_AT = ZonedDateTime.parse("2019-06-07T08:46:01.123456Z");

    @Test
    public void shouldHandleOutOfOrderEvents() throws InterruptedException {
        final String resourceExternalId = "rexid";
        aQueueEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("AUTHORISATION_SUCCESSFUL")
                .insert(rule.getSqsClient());

        // A created event with an earlier timestamp, sent later
        aQueueEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT.minusMinutes(1))
                .withEventType(EventType.PAYMENT_CREATED.name())
                .withDefaultEventDataForEventType(EventType.PAYMENT_CREATED.name())
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId)
                .then()
                .statusCode(200)
                .body("charge_id", is(resourceExternalId))
                .body("state.status", is("submitted"));
    }

    @Test
    public void shouldContinueToHandleMessagesFromQueueForDownstreamExceptions() throws InterruptedException {
        final String resourceExternalId = "rexid";
        final String resourceExternalId2 = "rexid2";
        aQueueEventFixture()
                .withResourceType(ResourceType.SERVICE) // throws PSQL exception. change to UNKNOWN when 'service' events are allowed
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("AUTHORISATION_SUCCESSFUL")
                .insert(rule.getSqsClient());

        aQueueEventFixture()
                .withResourceExternalId(resourceExternalId2)
                .withResourceType(ResourceType.PAYMENT)
                .withEventDate(CREATED_AT.minusMinutes(1))
                .withEventType(EventType.PAYMENT_CREATED.name())
                .withDefaultEventDataForEventType(EventType.PAYMENT_CREATED.name())
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId)
                .then()
                .statusCode(404);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId2)
                .then()
                .statusCode(200)
                .body("charge_id", is(resourceExternalId2))
                .body("state.status", is("created"));
    }
}
