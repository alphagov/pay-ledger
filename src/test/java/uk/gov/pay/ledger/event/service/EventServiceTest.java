package uk.gov.pay.ledger.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.event.model.response.CreateEventResponse;
import uk.gov.pay.ledger.util.fixture.EventFixture;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {
    @Mock
    EventDao mockEventDao;

    private static ObjectMapper objectMapper = Jackson.newObjectMapper();

    private EventService eventService;

    private Event event;

    private ZonedDateTime latestEventTime;
    private final String resourceExternalId = "resource_external_id";
    private Event event1;
    private Event event2;

    @BeforeEach
    public void setUp() {
        eventService = new EventService(mockEventDao);

        latestEventTime = ZonedDateTime.now().minusHours(1L);
        String eventDetails1 = "{ \"amount\": 1000}";
        event1 = EventFixture.anEventFixture()
                .withEventData(eventDetails1)
                .withResourceExternalId(resourceExternalId)
                .withEventDate(latestEventTime)
                .toEntity();
        String eventDetails2 = "{ \"amount\": 2000, \"description\": \"a payment\"}";
        event2 = EventFixture.anEventFixture()
                .withEventData(eventDetails2)
                .withResourceExternalId(resourceExternalId)
                .withEventDate(ZonedDateTime.now().minusHours(2L))
                .toEntity();
        lenient().when(mockEventDao.getEventsByResourceExternalId(resourceExternalId)).thenReturn(List.of(event1, event2));
    }

    @Test
    public void getEventDigestForResource_shouldUseFirstEventInListToPopulateEventDigestMetadata() {
        EventDigest eventDigest = eventService.getEventDigestForResource(event1);

        assertThat(eventDigest.getMostRecentEventTimestamp(), is(latestEventTime));
        assertThat(eventDigest.getMostRecentSalientEventType().get(), is(SalientEventType.PAYMENT_CREATED));
    }

    @Test
    public void laterEventsShouldOverrideEarlierEventsInEventDetailsDigest() {
        EventDigest eventDigest = eventService.getEventDigestForResource(event1);

        assertThat(eventDigest.getEventPayload().get("description"), is("a payment"));
        assertThat(eventDigest.getEventPayload().get("amount"), is(1000));
    }

    @Test
    public void shouldGetCorrectLatestSalientEventType() {
        String eventDetails1 = "{ \"amount\": 1000}";
        Event event1 = EventFixture.anEventFixture()
                .withEventData(eventDetails1)
                .withResourceExternalId(resourceExternalId)
                .withEventDate(latestEventTime)
                .toEntity();
        String eventDetails2 = "{ \"amount\": 2000, \"description\": \"a payment\"}";
        Event event2 = EventFixture.anEventFixture()
                .withEventData(eventDetails2)
                .withEventType("A_WEIRD_EVENT_THAT_SHOULD_NOT_BE_CONSIDERED_SALIENT")
                .withResourceExternalId(resourceExternalId)
                .withEventDate(ZonedDateTime.now().plusMinutes(2L))
                .toEntity();
        when(mockEventDao.getEventsByResourceExternalId(resourceExternalId)).thenReturn(List.of(event2, event1));

        EventDigest eventDigest = eventService.getEventDigestForResource(event1);

        assertThat(eventDigest.getMostRecentSalientEventType().get(), is(SalientEventType.PAYMENT_CREATED));
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