package uk.gov.pay.ledger.event.resource;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.entity.EventEntity;
import uk.gov.pay.ledger.queue.EventMessageHandler;
import uk.gov.pay.ledger.util.fixture.EventFixture;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(DropwizardExtensionsSupport.class)
public class EventResourceTest {
    private static final EventDao dao = mock(EventDao.class);
    private static final EventMessageHandler eventMessageHandler = mock(EventMessageHandler.class);
    private static final Long eventId = 1L;
    private static final String nonExistentId = "I'm not really here";
    private final EventEntity event = EventFixture.anEventFixture()
            .withId(eventId)
            .toEntity();

    public static final ResourceExtension resources = ResourceExtension.builder()
            .addResource(new EventResource(dao, eventMessageHandler))
            .build();

    @BeforeEach
    public void setup() {
        when(dao.getById(eventId)).thenReturn(Optional.of(event));
    }

    @Test
    public void shouldReturn400IfFromAndToDatesNotSuppliedToTickerEndpoint() {
        Response response = resources.target("/v1/event/ticker").request().get();
        assertThat(response.getStatus(), is(400));
    }
    
    @Test
    public void shouldReturn422IfEventTypeNotSuppliedToWriteEventsEndpoint() {
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        var params = new JSONArray();

        var event = new JSONObject()
                .put("service_id", "service-Id")
                .put("resource_type", "agreement")
                .put("live", false)
                .put("timestamp", now.toString())
                .put("event_details", new JSONObject())
                .put("resource_external_id", "agreement-Id");
        params.put(event);
        Response response = resources.target("/v1/event").request().post(Entity.json(params.toString()));
        assertThat(response.readEntity(String.class), containsString("Field [event_type] cannot be null"));
        assertThat(response.getStatus(), is(422));
    }

    @Test
    public void shouldReturn422IfTimestampNotSuppliedToWriteEventsEndpoint() {
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        var params = new JSONArray();

        var event = new JSONObject()
                .put("event_type", "AGREEMENT_CREATED")
                .put("service_id", "service-Id")
                .put("resource_type", "agreement")
                .put("live", false)
                .put("event_details", new JSONObject())
                .put("resource_external_id", "agreement-Id");
        params.put(event);
        Response response = resources.target("/v1/event").request().post(Entity.json(params.toString()));
        assertThat(response.readEntity(String.class), containsString("Field [timestamp] cannot be null"));
        assertThat(response.getStatus(), is(422));
    }

    @Test
    public void shouldReturn422IfResourceExternalIdNotSuppliedToWriteEventsEndpoint() {
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        var params = new JSONArray();

        var event = new JSONObject()
                .put("event_type", "AGREEMENT_CREATED")
                .put("service_id", "service-Id")
                .put("resource_type", "agreement")
                .put("live", false)
                .put("timestamp", now.toString())
                .put("event_details", new JSONObject());
        params.put(event);
        Response response = resources.target("/v1/event").request().post(Entity.json(params.toString()));
        assertThat(response.readEntity(String.class), containsString("Field [resource_external_id] cannot be null"));
        assertThat(response.getStatus(), is(422));
    }

    @Test
    public void shouldReturn422IfResourceTypeNotSuppliedToWriteEventsEndpoint() {
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        var params = new JSONArray();

        var event = new JSONObject()
                .put("event_type", "AGREEMENT_CREATED")
                .put("service_id", "service-Id")
                .put("live", false)
                .put("timestamp", now.toString())
                .put("event_details", new JSONObject())
                .put("resource_external_id", "agreement-Id");
        params.put(event);
        Response response = resources.target("/v1/event").request().post(Entity.json(params.toString()));
        assertThat(response.readEntity(String.class), containsString("Field [resource_type] cannot be null"));
        assertThat(response.getStatus(), is(422));
    }

    @Test
    public void shouldReturn422AndTwoMessagesIfEventTypeAndTimestampNotSuppliedToWriteEventsEndpoint() {
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        var params = new JSONArray();

        var event = new JSONObject()
                .put("service_id", "service-Id")
                .put("resource_type", "agreement")
                .put("live", false)
                .put("event_details", new JSONObject())
                .put("resource_external_id", "agreement-Id");
        params.put(event);
        Response response = resources.target("/v1/event").request().post(Entity.json(params.toString()));
        String responseString = response.readEntity(String.class);
        assertThat(responseString, containsString("Field [event_type] cannot be null"));
        assertThat(responseString, containsString("Field [timestamp] cannot be null"));
        assertThat(response.getStatus(), is(422));
    }
    
}
