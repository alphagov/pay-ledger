package uk.gov.pay.ledger.event.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.event.entity.EventEntity;
import uk.gov.pay.ledger.event.model.EventTicker;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.service.payments.commons.model.AuthorisationMode;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.time.ZonedDateTime.parse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.ZonedDateTimeTimestampMatcher.isDate;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

class EventDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private static final ZonedDateTime CREATED_AT = parse("2019-06-07T08:46:01.123456Z");
    private ObjectMapper objectMapper = new ObjectMapper();

    private EventDao eventDao;
    private ResourceTypeDao resourceTypeDao;
    private DatabaseTestHelper dbHelper;

    @BeforeEach
    void setUp() {
        eventDao = rule.getJdbi().onDemand(EventDao.class);
        resourceTypeDao = rule.getJdbi().onDemand(ResourceTypeDao.class);
        dbHelper = aDatabaseTestHelper(rule.getJdbi());
        dbHelper.truncateAllData();
    }

    @Test
    void shouldRedactReference() {
        String resourceExternalId = "52pfbqbta";
        String eventData = "{\"address\": \"Silicon Valley\", \"reference\": \"4242424242424242\"}";
        var paymentCreatedEvent = anEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventData(eventData)
                .withEventType("PAYMENT_CREATED");
        var paymentSucceededEvent = anEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventData(eventData)
                .withEventType("PAYMENT_SUCCEEDED");

        eventDao.insertEventWithResourceTypeId(paymentCreatedEvent.toEntity());
        eventDao.insertEventWithResourceTypeId(paymentSucceededEvent.toEntity());

        List<EventEntity> events = eventDao.getEventsByResourceExternalId(resourceExternalId);
        assertThat(events, hasSize(2));
        events.forEach(event -> assertThat(event.getEventData(), is(eventData)));

        eventDao.redactReference(paymentCreatedEvent.getResourceExternalId());
        eventDao.redactReference(paymentSucceededEvent.getResourceExternalId());

        events = eventDao.getEventsByResourceExternalId(resourceExternalId);
        assertThat(events, hasSize(2));
        events.forEach(event -> assertThat(event.getEventData(), is("{\"address\": \"Silicon Valley\", \"reference\": \"****************\"}")));
    }

    @Test
    void shouldInsertEvent() throws IOException {
        EventEntity event = anEventFixture()
                .withEventDate(CREATED_AT)
                .withParentResourceExternalId("parent-resource-id")
                .toEntity();

        eventDao.insertEventWithResourceTypeId(event);

        int resourceTypeId = resourceTypeDao.getResourceTypeIdByName(event.getResourceType().name());
        Map<String, Object> result = dbHelper.getEventByExternalId(event.getResourceExternalId());
        assertThat(result.get("sqs_message_id"), is(event.getSqsMessageId()));
        assertThat(result.get("resource_type_id"), is(resourceTypeId));
        assertThat(result.get("resource_external_id"), is(event.getResourceExternalId()));
        assertThat(result.get("parent_resource_external_id"), is(event.getParentResourceExternalId()));
        assertThat((Timestamp) result.get("event_date"), isDate(CREATED_AT));
        assertThat(result.get("event_type").toString(), is(event.getEventType()));
        assertThat(objectMapper.readTree(result.get("event_data").toString()), is(objectMapper.readTree(event.getEventData())));
    }

    @Test
    void shouldInsertNotExistingEvent() throws IOException {
        EventEntity event = anEventFixture()
                .withEventDate(CREATED_AT)
                .toEntity();

        Optional<Long> status = eventDao.insertEventIfDoesNotExistWithResourceTypeId(event);

        assertTrue(status.isPresent());

        int resourceTypeId = resourceTypeDao.getResourceTypeIdByName(event.getResourceType().name());
        Map<String, Object> result = dbHelper.getEventByExternalId(event.getResourceExternalId());
        assertThat(result.get("sqs_message_id"), is(event.getSqsMessageId()));
        assertThat(result.get("resource_type_id"), is(resourceTypeId));
        assertThat(result.get("resource_external_id"), is(event.getResourceExternalId()));
        assertThat((Timestamp) result.get("event_date"), isDate(CREATED_AT));
        assertThat(result.get("event_type").toString(), is(event.getEventType()));
        assertThat(objectMapper.readTree(result.get("event_data").toString()), is(objectMapper.readTree(event.getEventData())));
    }

    @Test
    void shouldInsertDuplicateEventWithDifferentTimestamp() {
        EventEntity event = anEventFixture()
                .insert(rule.getJdbi())
                .toEntity();
        EventEntity duplicateEvent = anEventFixture()
                .from(event)
                .withEventDate(CREATED_AT)
                .toEntity();

        Optional<Long> status = eventDao.insertEventIfDoesNotExistWithResourceTypeId(duplicateEvent);

        assertThat(status.isPresent(), is(true));

        List<Map<String, Object>> results = dbHelper.getEventsByExternalId(event.getResourceExternalId());
        assertThat(results.size(), is(2));
        assertThat(results.get(0).get("sqs_message_id"), is(duplicateEvent.getSqsMessageId()));
        assertThat(results.get(1).get("sqs_message_id"), is(event.getSqsMessageId()));
    }

    @Test
    void shouldNotInsertDuplicateEvent() throws IOException {
        EventEntity event = anEventFixture()
                .withEventDate(CREATED_AT)
                .insert(rule.getJdbi())
                .toEntity();
        EventEntity duplicateEvent = anEventFixture()
                .from(event)
                .withSQSMessageId(RandomStringUtils.randomAlphanumeric(50))
                .withEventDate(CREATED_AT)
                .withEventData("{\"event_data\": \"duplicate event data\"}")
                .toEntity();

        Optional<Long> status = eventDao.insertEventIfDoesNotExistWithResourceTypeId(duplicateEvent);

        assertThat(status.isPresent(), is(false));

        int resourceTypeId = resourceTypeDao.getResourceTypeIdByName(event.getResourceType().name());
        Map<String, Object> result = dbHelper.getEventByExternalId(event.getResourceExternalId());
        int numberOfEvents = dbHelper.getEventsCountByExternalId(event.getResourceExternalId());
        assertThat(numberOfEvents, is(1));
        assertThat(result.get("sqs_message_id"), is(event.getSqsMessageId()));
        assertThat(result.get("resource_type_id"), is(resourceTypeId));
        assertThat(result.get("resource_external_id"), is(event.getResourceExternalId()));
        assertThat((Timestamp) result.get("event_date"), isDate(event.getEventDate()));
        assertThat(result.get("event_type").toString(), is(event.getEventType()));
        assertThat(objectMapper.readTree(result.get("event_data").toString()), is(objectMapper.readTree(event.getEventData())));
    }

    @Test
    void shouldFindEvent() {
        EventEntity event = anEventFixture()
                .insert(rule.getJdbi())
                .toEntity();

        Optional<EventEntity> optionalEvent = eventDao.getById(event.getId());
        assertThat(optionalEvent.isPresent(), is(true));
        EventEntity retrievedEvent = optionalEvent.get();
        assertThat(retrievedEvent.getId(), is(event.getId()));
        assertThat(retrievedEvent.getResourceExternalId(), is(event.getResourceExternalId()));
        assertThat(retrievedEvent.getResourceType().name(), is(event.getResourceType().name()));
        assertThat(retrievedEvent.getSqsMessageId(), is(event.getSqsMessageId()));
        assertThat(retrievedEvent.getEventType(), is(event.getEventType()));
        assertThat(retrievedEvent.getEventDate(), is(event.getEventDate()));
        assertThat(retrievedEvent.getEventData(), is(event.getEventData()));
    }

    @Test
    void shouldGetAllEventsForResourceExternalIdInDescendingDateOrder() {
        final String resourceExternalId = "resourceExternalId";

        EventEntity earliestEvent = anEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(ZonedDateTime.now().minusHours(4))
                .insert(rule.getJdbi())
                .toEntity();

        EventEntity latestEvent = anEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(ZonedDateTime.now().minusHours(1))
                .insert(rule.getJdbi())
                .toEntity();

        EventEntity middleEvent = anEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(ZonedDateTime.now().minusHours(2))
                .insert(rule.getJdbi())
                .toEntity();

        List<EventEntity> events = eventDao.getEventsByResourceExternalId(resourceExternalId);

        assertThat(events.size(), is(3));
        assertThat(events.get(0).getId(), is(latestEvent.getId()));
        assertThat(events.get(1).getId(), is(middleEvent.getId()));
        assertThat(events.get(2).getId(), is(earliestEvent.getId()));
    }

    @Test
    void shouldGetEmptyListWhenNoEventsWithResourceExternalId() {
        List<EventEntity> events = eventDao.getEventsByResourceExternalId("no_events_for_this_id");

        assertThat(events.size(), is(0));
    }

    @Test
    void findEventsForExternalIdsShouldFilterEventsByMultipleExternalIds() {
        EventEntity event1 = anEventFixture()
                .withResourceExternalId("external-id-1")
                .insert(rule.getJdbi())
                .toEntity();
        EventEntity event2 = anEventFixture()
                .withResourceExternalId("external-id-2")
                .withEventDate(event1.getEventDate().plusDays(1))
                .insert(rule.getJdbi())
                .toEntity();
        anEventFixture()
                .withResourceExternalId("external-id-3")
                .withEventDate(event2.getEventDate().plusDays(1))
                .insert(rule.getJdbi());

        List<EventEntity> eventList = eventDao.findEventsForExternalIds(Set.of("external-id-1", "external-id-2"));

        assertThat(eventList.size(), is(2));

        assertThat(eventList.get(0).getResourceExternalId(), is(event1.getResourceExternalId()));
        assertThat(eventList.get(1).getResourceExternalId(), is(event2.getResourceExternalId()));
    }

    @Test
    void findEventsForExternalIds_ShouldReturnEmptyListIfNoRecordsFound() {
        List<EventEntity> eventList = eventDao.findEventsForExternalIds(Set.of("some-ext-id-1", "some-ext-id-2"));
        assertThat(eventList.size(), is(0));
    }

    @Test
    void findEventsTickerFromDate_ShouldGetAllEventsFromDateSpecified() {
        aTransactionFixture()
                .withExternalId("external-id-1")
                .withGatewayAccountId("100")
                .withAmount(200L)
                .withLive(true)
                .withMoto(true)
                .withSource("CARD_API")
                .withAuthorisationMode(AuthorisationMode.AGREEMENT)
                .withServiceId("service-id-event-ticker")
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi())
                .toEntity();

        EventEntity event1 = anEventFixture()
                .withResourceExternalId("external-id-1")
                .withEventType("PAYMENT_CREATED")
                .insert(rule.getJdbi())
                .toEntity();
        EventEntity event2 = anEventFixture()
                .withResourceExternalId("external-id-1")
                .withEventDate(event1.getEventDate().minusDays(1))
                .insert(rule.getJdbi())
                .toEntity();
        anEventFixture()
                .withResourceExternalId("external-id-1")
                .withEventDate(event2.getEventDate().minusDays(1))
                .insert(rule.getJdbi())
                .toEntity();

        List<EventTicker> eventTickers = eventDao.findEventsTickerFromDate(
                event1.getEventDate().minusHours(1),
                event1.getEventDate().plusHours(1)
        );
        assertThat(eventTickers.size(), is(1));
        assertThat(eventTickers.get(0).getGatewayAccountId(), is("100"));
        assertThat(eventTickers.get(0).getResourceExternalId(), is("external-id-1"));
        assertThat(eventTickers.get(0).getEventType(), is("PAYMENT_CREATED"));
        assertThat(eventTickers.get(0).getServiceExternalId(), is("service-id-event-ticker"));
        assertThat(eventTickers.get(0).getWalletType(), is("APPLE_PAY"));
        assertThat(eventTickers.get(0).getPaymentProvider(), is("sandbox"));
        assertThat(eventTickers.get(0).getSource(), is("CARD_API"));
        assertThat(eventTickers.get(0).getIsMoto(), is(true));
        assertThat(eventTickers.get(0).getIsRecurring(), is(true));
        assertThat(eventTickers.get(0).getAmount(), is(200L));
    }

    @Nested
    @DisplayName("DeleteEventsForTransactions")
    class TestDeleteEventsForTransactions {
        @Test
        void shouldDeleteEventsForListOfTransactionIDs() {
            EventEntity event1 = anEventFixture()
                    .withResourceExternalId("external-id-1")
                    .withEventType("PAYMENT_CREATED")
                    .insert(rule.getJdbi())
                    .toEntity();
            EventEntity event2 = anEventFixture()
                    .withResourceExternalId("external-id-1")
                    .withEventDate(event1.getEventDate().minusDays(1))
                    .insert(rule.getJdbi())
                    .toEntity();
            EventEntity event3 = anEventFixture()
                    .withResourceExternalId("external-id-2")
                    .withEventDate(event1.getEventDate().minusDays(1))
                    .insert(rule.getJdbi())
                    .toEntity();

            int noOfEventsRemoved = eventDao.deleteEventsForTransactions(List.of("external-id-1", "external-id-2"));

            assertThat(noOfEventsRemoved, is(3));

            List<EventEntity> noOfEventsStillInDatabase = eventDao.findEventsForExternalIds(Set.of("external-id-1", "external-id-2"));
            assertThat(noOfEventsStillInDatabase.size(), is(0));
        }

        @Test
        void shouldNotDeleteEventsNotMatchingTransactionIds() {
            EventEntity event1 = anEventFixture()
                    .withResourceExternalId("external-id-1")
                    .withEventType("PAYMENT_CREATED")
                    .insert(rule.getJdbi())
                    .toEntity();
            EventEntity event2 = anEventFixture()
                    .withResourceExternalId("external-id-2")
                    .withEventDate(event1.getEventDate().minusDays(1))
                    .insert(rule.getJdbi())
                    .toEntity();

            int noOfEventsRemoved = eventDao.deleteEventsForTransactions(List.of("some-tx-id"));

            assertThat(noOfEventsRemoved, is(0));

            List<EventEntity> eventsNotRemoved = eventDao.findEventsForExternalIds(Set.of("external-id-1", "external-id-2"));
            assertThat(eventsNotRemoved.size(), is(2));
        }
    }
}
