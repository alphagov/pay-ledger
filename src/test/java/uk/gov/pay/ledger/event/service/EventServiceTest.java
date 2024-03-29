package uk.gov.pay.ledger.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.entity.EventEntity;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.event.model.response.CreateEventResponse;
import uk.gov.pay.ledger.exception.EmptyEventsException;
import uk.gov.pay.ledger.util.fixture.EventFixture;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.event.model.ResourceType.AGREEMENT;
import static uk.gov.pay.ledger.event.model.ResourceType.PAYMENT;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    @Mock
    EventDao mockEventDao;

    private EventService eventService;

    private EventEntity event;

    private ZonedDateTime latestEventTime;
    private final String resourceExternalId = "resource_external_id";
    private EventEntity event1;
    private EventEntity event2;

    @BeforeEach
    void setUp() {
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
    void getEventDigestForResource_shouldUseFirstEventInListToPopulateEventDigestMetadata() {
        EventDigest eventDigest = eventService.getEventDigestForResource(event1);

        assertThat(eventDigest.getMostRecentEventTimestamp(), is(latestEventTime));
        assertThat(eventDigest.getMostRecentSalientEventType().get(), is(SalientEventType.PAYMENT_CREATED));
    }

    @Test
    void laterEventsShouldOverrideEarlierEventsInEventDetailsDigest() {
        EventDigest eventDigest = eventService.getEventDigestForResource(event1);

        assertThat(eventDigest.getEventAggregate().get("description"), is("a payment"));
        assertThat(eventDigest.getEventAggregate().get("amount"), is(1000));
    }

    @Test
    void shouldGetCorrectLatestSalientEventType() {
        String eventDetails1 = "{ \"amount\": 1000}";
        EventEntity event1 = EventFixture.anEventFixture()
                .withEventData(eventDetails1)
                .withResourceExternalId(resourceExternalId)
                .withEventDate(latestEventTime)
                .toEntity();
        String eventDetails2 = "{ \"amount\": 2000, \"description\": \"a payment\"}";
        EventEntity event2 = EventFixture.anEventFixture()
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
    void createIfDoesNotExistReturnsSuccessfulCreatedResponse() {
        when(mockEventDao.insertEventIfDoesNotExistWithResourceTypeId(event)).thenReturn(Optional.of(1L));

        CreateEventResponse response = eventService.createIfDoesNotExist(event);

        assertTrue(response.isSuccessful());
        assertThat(response.getState(), is(CreateEventResponse.CreateEventState.INSERTED));
    }

    @Test
    void createIfDoesNotExistReturnsSuccessfulIgnoredResponse() {
        when(mockEventDao.insertEventIfDoesNotExistWithResourceTypeId(event)).thenReturn(Optional.empty());

        CreateEventResponse response = eventService.createIfDoesNotExist(event);

        assertTrue(response.isSuccessful());
        assertThat(response.getState(), is(CreateEventResponse.CreateEventState.IGNORED));
    }

    @Test
    void createIfDoesNotExistReturnsNotSuccessfulResponse() {
        when(mockEventDao.insertEventIfDoesNotExistWithResourceTypeId(event))
                .thenThrow(new RuntimeException("forced failure"));

        CreateEventResponse response = eventService.createIfDoesNotExist(event);

        assertFalse(response.isSuccessful());
        assertThat(response.getState(), is(CreateEventResponse.CreateEventState.ERROR));
        assertThat(response.getErrorMessage(), is("forced failure"));
    }

    @Nested
    class getEventDigestForResourceAndType {
        @Test
        void shouldReturnEventDigestForResourceAndResourceTypeCorrectly() {
            ZonedDateTime eventTime = Instant.now().atZone(UTC);
            event1 = EventFixture.anEventFixture()
                    .withEventData("{}")
                    .withResourceExternalId(resourceExternalId)
                    .withEventDate(eventTime)
                    .withEventType("AGREEMENT_SET_UP")
                    .withResourceType(AGREEMENT)
                    .toEntity();
            when(mockEventDao.getEventsByResourceExternalId(resourceExternalId)).thenReturn(List.of(event1));

            EventDigest eventDigest = eventService.getEventDigestForResourceAndType(resourceExternalId, AGREEMENT);

            assertThat(eventDigest.getMostRecentEventTimestamp(), is(eventTime));
            assertThat(eventDigest.getMostRecentSalientEventType().get(), is(SalientEventType.AGREEMENT_SET_UP));
        }

        @Test
        void shouldThrowExceptionIfNoEventsAreFoundForResourceExternalId() {
            when(mockEventDao.getEventsByResourceExternalId(resourceExternalId)).thenReturn(List.of());

            assertThrows(EmptyEventsException.class, () ->
                    eventService.getEventDigestForResourceAndType(resourceExternalId, AGREEMENT));
        }

        @Test
        void shouldThrowExceptionIfNoEventsAreFoundForResourceType() {
            ZonedDateTime eventTime = Instant.now().atZone(UTC);
            event1 = EventFixture.anEventFixture()
                    .withEventData("{}")
                    .withResourceExternalId(resourceExternalId)
                    .withEventDate(eventTime)
                    .withEventType("PAYMENT_CREATED")
                    .withResourceType(PAYMENT)
                    .toEntity();
            when(mockEventDao.getEventsByResourceExternalId(resourceExternalId)).thenReturn(List.of(event1));

            assertThrows(EmptyEventsException.class, () ->
                    eventService.getEventDigestForResourceAndType(resourceExternalId, AGREEMENT));
        }
    }
}
