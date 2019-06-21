package uk.gov.pay.ledger.queue;

import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;

import java.io.IOException;
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
    public void shouldInsertEvent() throws IOException, InterruptedException {
        Event event = aQueueEventFixture()
                .withEventDate(CREATED_AT)
                .insert(rule.getSqsClient())
                .toEntity();

        Thread.sleep(1000);

        String resourceExternalId = event.getResourceExternalId();
        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId)
                .then()
                .statusCode(200)
                .body("external_id", is(resourceExternalId))
                .body("state", is("Created"));
    }
}
