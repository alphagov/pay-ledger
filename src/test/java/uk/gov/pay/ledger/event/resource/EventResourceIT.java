package uk.gov.pay.ledger.event.resource;

import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;

import javax.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class EventResourceIT {
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
                .body("event_type", is(event.getEventType().toString()))
                .body("event_data", is(event.getEventData()));
    }

    @Test
    public void shouldGetEvenTicker() {
        aTransactionFixture()
                .withExternalId("an-external-id")
                .withLive(true)
                .insert(rule.getJdbi())
                .toEntity();

        Event event = anEventFixture()
                .withResourceExternalId("an-external-id")
                .insert(rule.getJdbi())
                .toEntity();

        anEventFixture()
                .withResourceExternalId("an-external-id")
                .withEventDate(event.getEventDate().minusHours(1))
                .insert(rule.getJdbi())
                .toEntity();

        anEventFixture()
                .withResourceExternalId("an-external-id")
                .withEventDate(event.getEventDate().plusHours(1))
                .insert(rule.getJdbi())
                .toEntity();

        given().port(port)
                .contentType(JSON)
                .queryParam("from_date", event.getEventDate().minusMinutes(1).toString())
                .queryParam("to_date", event.getEventDate().plusMinutes(1).toString())
                .get("/v1/event/ticker")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("[0].resource_external_id", is("an-external-id"))
                .body("[0].event_type", is("PAYMENT_CREATED"))
                .body("size()", is(1));
    }
}
