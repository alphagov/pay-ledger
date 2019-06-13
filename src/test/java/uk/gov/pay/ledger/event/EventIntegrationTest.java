package uk.gov.pay.ledger.event;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.rules.AppWithPostgresAndSqsRule;

import javax.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.pay.ledger.utils.fixtures.EventFixture.anEventFixture;

@Ignore
public class EventIntegrationTest {
    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private Integer port = rule.getAppRule().getLocalPort();

    @Test
    public void shouldGetEventFromDB() {
        Event event = anEventFixture()
                .insert(rule.getJdbi())
                .toEntity();

        given().port(port)
                .contentType(JSON)
                .get("/v1/event/" + event.getId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("resource_external_id", is(event.getResourceExternalId()))
                .body("resource_type", is(event.getResourceType().name().toLowerCase()))
                .body("sqs_message_id", is(event.getSqsMessageId()))
                .body("event_type", is(event.getEventType()))
                .body("event_data", is(event.getEventData()));
    }
}
