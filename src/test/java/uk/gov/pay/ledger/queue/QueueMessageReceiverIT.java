package uk.gov.pay.ledger.queue;

import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.event.model.SalientEventType;
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
                .withEventType("PAYMENT_DETAILS_EVENT")
                .withEventData(SalientEventType.PAYMENT_DETAILS_EVENT)
                .insert(rule.getSqsClient());

        // A created event with an earlier timestamp, sent later
        aQueueEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT.minusMinutes(1))
                .withEventData(SalientEventType.PAYMENT_CREATED)
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
}
