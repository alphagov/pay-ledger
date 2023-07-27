package uk.gov.pay.ledger.event.resource;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.event.entity.EventEntity;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import javax.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;
import static org.hamcrest.MatcherAssert.assertThat;

public class EventResourceIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private Integer port = rule.getAppRule().getLocalPort();

    private DatabaseTestHelper databaseTestHelper;

    @BeforeEach
    public void setUp() {
        databaseTestHelper = DatabaseTestHelper.aDatabaseTestHelper(rule.getJdbi());
        databaseTestHelper.truncateAllData();
    }

    @Test
    public void shouldWriteEvent() {
        var params = new JSONArray();

        var event = new JSONObject()
                .put("event_type", "PAYMENT_CREATED")
                .put("service_id", "a-service-id")
                .put("resource_type", "payment")
                .put("live", false)
                .put("timestamp", "2022-03-23T22:08:02.123456Z")
                .put("event_details", new JSONObject())
                .put("resource_external_id", "a-valid-external-id");
        params.put(event);

        given().port(port)
                .contentType(JSON)
                .body(params.toString())
                .post("/v1/event")
                .then()
                .statusCode(Response.Status.ACCEPTED.getStatusCode());

        var results = databaseTestHelper.getEventsByExternalId("a-valid-external-id");
        assertThat(results.size(), is(1));
    }

    @Test
    public void shouldWriteNoEventsIfAnyFail() {
        var params = new JSONArray();

        var aValidEvent = new JSONObject()
                .put("event_type", "PAYMENT_CREATED")
                .put("service_id", "a-service-id")
                .put("resource_type", "payment")
                .put("live", false)
                .put("timestamp", "2022-03-23T22:08:02.123456Z")
                .put("event_details", new JSONObject())
                .put("resource_external_id", "a-valid-external-id");
        var anInvalidEventMissingResourceId = new JSONObject()
                .put("event_type", "PAYMENT_UPDATED")
                .put("service_id", "a-service-id")
                .put("resource_type", "payment")
                .put("live", false)
                .put("timestamp", "2022-03-23T23:08:02.123456Z")
                .put("event_details", new JSONObject());
        params.put(aValidEvent);
        params.put(anInvalidEventMissingResourceId);

        given().port(port)
                .contentType(JSON)
                .body(params.toString())
                .post("/v1/event")
                .then()
                .statusCode(422)
                .body("message", contains("Field [resource_external_id] cannot be null"));

        // assert no events are persisted
        var eventsResult = databaseTestHelper.getEventsByExternalId("a-valid-external-id");
        assertThat(eventsResult.size(), is(0));

        // assert no projections persisted
        var transactionsResult = databaseTestHelper.getAllTransactions();
        assertThat(transactionsResult.size(), is(0));
    }

    @Test
    public void shouldGetEvenTicker() {
        aTransactionFixture()
                .withExternalId("an-external-id")
                .withLive(true)
                .insert(rule.getJdbi())
                .toEntity();

        EventEntity event = anEventFixture()
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
