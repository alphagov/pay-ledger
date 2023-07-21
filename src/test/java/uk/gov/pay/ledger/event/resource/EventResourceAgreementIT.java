package uk.gov.pay.ledger.event.resource;

import org.exparity.hamcrest.date.ZonedDateTimeMatchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.agreement.dao.AgreementDao;
import uk.gov.pay.ledger.agreement.entity.AgreementEntity;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.EventFixture;

import javax.ws.rs.core.Response;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EventResourceAgreementIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private Integer port = rule.getAppRule().getLocalPort();
    private DatabaseTestHelper databaseTestHelper;
    private final EventDao eventDao = rule.getJdbi().onDemand(EventDao.class);
    private final AgreementDao agreementDao = new AgreementDao(rule.getJdbi());
    private final String serviceId = "service-id";
    private final String agreementId = "agreement-id";

    @BeforeEach
    public void setUp() {
        databaseTestHelper = DatabaseTestHelper.aDatabaseTestHelper(rule.getJdbi());
        databaseTestHelper.truncateAllData();
    }

    @Test
    public void shouldWriteCancelledEvent() {
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        insertAgreementCreatedEventFixture();
        
        var params = new JSONArray();
        var event = new JSONObject()
                .put("event_type", "AGREEMENT_CANCELLED_BY_USER")
                .put("service_id", serviceId)
                .put("resource_type", "agreement")
                .put("live", false)
                .put("timestamp", now.toString())
                .put("event_details", new JSONObject()
                        .put("user_email", "jdoe@example.org")
                        .put("cancelled_date", now.toString()))
                .put("resource_external_id", agreementId);
        params.put(event);

        given().port(port)
                .contentType(JSON)
                .body(params.toString())
                .post("/v1/event")
                .then()
                .statusCode(Response.Status.ACCEPTED.getStatusCode());

        var results = databaseTestHelper.getEventsByExternalId(agreementId);
        assertThat(results.size(), is(2));

        var agreement = agreementDao.findByExternalId(agreementId);
        assertThat(agreement.isPresent(), is(true));
        AgreementEntity agreementEntity = agreement.get();
        assertThat(agreementEntity.getCancelledByUserEmail(), is("jdoe@example.org"));
        assertThat(agreementEntity.getCancelledDate(), is(ZonedDateTimeMatchers.within(1, ChronoUnit.SECONDS, now)));
    }
    
    private void insertAgreementCreatedEventFixture() {
        var agreementEvent = EventFixture.anEventFixture()
                .withResourceExternalId(agreementId)
                .withServiceId(serviceId)
                .withResourceType(ResourceType.AGREEMENT)
                .withEventType("CREATED")
                .withEventData("{\"data\": \"Event 1\"}");
        eventDao.insertEventWithResourceTypeId(agreementEvent.toEntity());
    }
    
}
