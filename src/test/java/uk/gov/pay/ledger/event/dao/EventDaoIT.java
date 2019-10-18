package uk.gov.pay.ledger.event.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.ZonedDateTimeTimestampMatcher.isDate;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;

public class EventDaoIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private static final ZonedDateTime CREATED_AT = ZonedDateTime.parse("2019-06-07T08:46:01.123456Z");
    private ObjectMapper objectMapper = new ObjectMapper();

    private EventDao eventDao;
    private ResourceTypeDao resourceTypeDao;
    private DatabaseTestHelper dbHelper;

    @Before
    public void setUp() {
        eventDao = rule.getJdbi().onDemand(EventDao.class);
        resourceTypeDao = rule.getJdbi().onDemand(ResourceTypeDao.class);
        dbHelper = aDatabaseTestHelper(rule.getJdbi());
    }

    @Test
    public void shouldInsertEvent() throws IOException {
        Event event = anEventFixture()
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
        assertThat(result.get("event_type").toString(), is(event.getEventType().toString()));
        assertThat(objectMapper.readTree(result.get("event_data").toString()), is(objectMapper.readTree(event.getEventData())));
    }

    @Test
    public void shouldInsertNotExistingEvent() throws IOException {
        Event event = anEventFixture()
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
        assertThat(result.get("event_type").toString(), is(event.getEventType().toString()));
        assertThat(objectMapper.readTree(result.get("event_data").toString()), is(objectMapper.readTree(event.getEventData())));
    }

    @Test
    public void shouldInsertDuplicateEventWithDifferentTimestamp() {
        Event event = anEventFixture()
                .insert(rule.getJdbi())
                .toEntity();
        Event duplicateEvent = anEventFixture()
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
    public void shouldNotInsertDuplicateEvent() throws IOException {
        Event event = anEventFixture()
                .withEventDate(CREATED_AT)
                .insert(rule.getJdbi())
                .toEntity();
        Event duplicateEvent = anEventFixture()
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
        assertThat(result.get("event_type").toString(), is(event.getEventType().toString()));
        assertThat(objectMapper.readTree(result.get("event_data").toString()), is(objectMapper.readTree(event.getEventData())));
    }

    @Test
    public void shouldFindEvent() {
        Event event = anEventFixture()
                .insert(rule.getJdbi())
                .toEntity();

        Optional<Event> optionalEvent = eventDao.getById(event.getId());
        assertThat(optionalEvent.isPresent(), is(true));
        Event retrievedEvent = optionalEvent.get();
        assertThat(retrievedEvent.getId(), is(event.getId()));
        assertThat(retrievedEvent.getResourceExternalId(), is(event.getResourceExternalId()));
        assertThat(retrievedEvent.getResourceType().name(), is(event.getResourceType().name()));
        assertThat(retrievedEvent.getSqsMessageId(), is(event.getSqsMessageId()));
        assertThat(retrievedEvent.getEventType().toString(), is(event.getEventType().toString()));
        assertThat(retrievedEvent.getEventDate(), is(event.getEventDate()));
        assertThat(retrievedEvent.getEventData(), is(event.getEventData()));
    }

    @Test
    public void shouldGetAllEventsForResourceExternalIdInDescendingDateOrder() {
        final String resourceExternalId = "resourceExternalId";

        Event earliestEvent = anEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(ZonedDateTime.now().minusHours(4))
                .insert(rule.getJdbi())
                .toEntity();

        Event latestEvent = anEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(ZonedDateTime.now().minusHours(1))
                .insert(rule.getJdbi())
                .toEntity();

        Event middleEvent = anEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(ZonedDateTime.now().minusHours(2))
                .insert(rule.getJdbi())
                .toEntity();

        List<Event> events = eventDao.getEventsByResourceExternalId(resourceExternalId);

        assertThat(events.size(), is(3));
        assertThat(events.get(0).getId(), is(latestEvent.getId()));
        assertThat(events.get(1).getId(), is(middleEvent.getId()));
        assertThat(events.get(2).getId(), is(earliestEvent.getId()));
    }

    @Test
    public void shouldGetEmptyListWhenNoEventsWithResourceExternalId() {
        List<Event> events = eventDao.getEventsByResourceExternalId("no_events_for_this_id");

        assertThat(events.size(), is(0));
    }

    @Test
    public void findEventsForExternalIdsShouldFilterEventsByMultipleExternalIds() {
        Event event1 = anEventFixture()
                .withResourceExternalId("external-id-1")
                .insert(rule.getJdbi())
                .toEntity();
        Event event2 = anEventFixture()
                .withResourceExternalId("external-id-2")
                .withEventDate(event1.getEventDate().plusDays(1))
                .insert(rule.getJdbi())
                .toEntity();
        Event event3 = anEventFixture()
                .withResourceExternalId("external-id-3")
                .withEventDate(event2.getEventDate().plusDays(1))
                .insert(rule.getJdbi())
                .toEntity();

        List<Event> eventList = eventDao.findEventsForExternalIds(Set.of("external-id-1", "external-id-2"));
        assertThat(eventList.size(), is(2));

        assertThat(eventList.get(0).getResourceExternalId(), is(event1.getResourceExternalId()));
        assertThat(eventList.get(1).getResourceExternalId(), is(event2.getResourceExternalId()));
    }

    @Test
    public void findEventsForExternalIds_ShouldReturnEmptyListIfNoRecordsFound() {
        List<Event> eventList = eventDao.findEventsForExternalIds(Set.of("some-ext-id-1", "some-ext-id-2"));
        assertThat(eventList.size(), is(0));
    }

    @Test
    public void eventDetailsAreUpdated_IfEventAlreadyExists(){
        Event event = anEventFixture()
                .withResourceExternalId("key-value-test-id")
                .insert(rule.getJdbi())
                .toEntity();
        Event event2 = anEventFixture()
                .withResourceExternalId(event.getResourceExternalId())
                .withEventData("{\"key\": \"value\"}")
                .withEventDate(event.getEventDate())
                .toEntity();
        eventDao.updateIfExistsWithResourceTypeId(event2);
        assertThat(eventDao.getById(event.getId()).get().getEventData(), is("{\"key\": \"value\"}"));
    }
}
