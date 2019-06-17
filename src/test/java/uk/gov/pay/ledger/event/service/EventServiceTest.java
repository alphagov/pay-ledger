package uk.gov.pay.ledger.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.EventType;
import uk.gov.pay.ledger.event.model.response.CreateEventResponse;
import uk.gov.pay.ledger.util.fixture.EventFixture;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventServiceTest {
    @Mock
    EventDao mockEventDao;

    private static ObjectMapper objectMapper = Jackson.newObjectMapper();

    private EventService eventService;

    private Event event;

    private ZonedDateTime latestEventTime;
    private final String resourceExternalId = "resource_external_id";

    @Before
    public void setUp() {
        eventService = new EventService(mockEventDao);

        latestEventTime = ZonedDateTime.now().minusHours(1L);
        String eventDetails1 = "{ \"amount\": 1000}";
        Event event1 = EventFixture.anEventFixture()
                .withEventData(eventDetails1)
                .withResourceExternalId(resourceExternalId)
                .withEventDate(latestEventTime)
                .toEntity();
        String eventDetails2 = "{ \"amount\": 2000, \"description\": \"a payment\"}";
        Event event2 = EventFixture.anEventFixture()
                .withEventData(eventDetails2)
                .withResourceExternalId(resourceExternalId)
                .withEventDate(ZonedDateTime.now().minusHours(2L))
                .toEntity();
        when(mockEventDao.getEventsByResourceExternalId(resourceExternalId)).thenReturn(List.of(event1, event2));
    }

    @Test
    public void getEventDigestForResource_shouldUseFirstEventInListToPopulateEventDigestMetadata() {
        EventDigest eventDigest = eventService.getEventDigestForResource(resourceExternalId);

        assertThat(eventDigest.getMostRecentEventTimestamp(), is(latestEventTime));
        assertThat(eventDigest.getMostRecentEventType(), is(EventType.PAYMENT_CREATED));
    }

    @Test
    public void laterEventsShouldOverrideEarlierEventsInEventDetailsDigest() {
        EventDigest eventDigest = eventService.getEventDigestForResource(resourceExternalId);

        assertThat(eventDigest.getEventDetailsDigest().get("description"), is("a payment"));
        assertThat(eventDigest.getEventDetailsDigest().get("amount"), is(1000));
    }

    @Test
    public void createIfDoesNotExistReturnsSuccessfulCreatedResponse() {
        when(mockEventDao.insertEventIfDoesNotExistWithResourceTypeId(event)).thenReturn(Optional.of(1L));

        CreateEventResponse response = eventService.createIfDoesNotExist(event);

        assertTrue(response.isSuccessful());
        assertThat(response.getState(), is(CreateEventResponse.CreateEventState.INSERTED));
    }

    @Test
    public void createIfDoesNotExistReturnsSuccessfulIgnoredResponse() {
        when(mockEventDao.insertEventIfDoesNotExistWithResourceTypeId(event)).thenReturn(Optional.empty());

        CreateEventResponse response = eventService.createIfDoesNotExist(event);

        assertTrue(response.isSuccessful());
        assertThat(response.getState(), is(CreateEventResponse.CreateEventState.IGNORED));
    }

    @Test
    public void createIfDoesNotExistReturnsNotSuccessfulResponse() {
        when(mockEventDao.insertEventIfDoesNotExistWithResourceTypeId(event))
                .thenThrow(new RuntimeException("forced failure"));

        CreateEventResponse response = eventService.createIfDoesNotExist(event);

        assertFalse(response.isSuccessful());
        assertThat(response.getState(), is(CreateEventResponse.CreateEventState.ERROR));
        assertThat(response.getErrorMessage(), is("forced failure"));
    }
}