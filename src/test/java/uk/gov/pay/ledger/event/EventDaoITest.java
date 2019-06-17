package uk.gov.pay.ledger.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.dao.ResourceTypeDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.ZonedDateTimeTimestampMatcher.isDate;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;

@Ignore
public class EventDaoITest {

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
                .toEntity();

        eventDao.insertEventWithResourceTypeId(event);

        int resourceTypeId = resourceTypeDao.getResourceTypeIdByName(event.getResourceType().name());
        Map<String, Object> result = dbHelper.getEventByExternalId(event.getResourceExternalId());
        assertThat(result.get("sqs_message_id"), is(event.getSqsMessageId()));
        assertThat(result.get("resource_type_id"), is(resourceTypeId));
        assertThat(result.get("resource_external_id"), is(event.getResourceExternalId()));
        assertThat((Timestamp) result.get("event_date"), isDate(CREATED_AT));
        assertThat(result.get("event_type"), is(event.getEventType()));
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
        assertThat(result.get("event_type"), is(event.getEventType()));
        assertThat(objectMapper.readTree(result.get("event_data").toString()), is(objectMapper.readTree(event.getEventData())));
    }

    @Test
    public void shouldNotInsertDuplicateEvent() throws IOException {
        Event event = anEventFixture()
                .insert(rule.getJdbi())
                .toEntity();
        Event duplicateEvent = anEventFixture()
                .from(event)
                .withSQSMessageId(RandomStringUtils.randomAlphanumeric(50))
                .withEventDate(CREATED_AT)
                .withEventData("{\"event_data\": \"duplicate event data\"}")
                .toEntity();

        Optional<Long> status = eventDao.insertEventIfDoesNotExistWithResourceTypeId(duplicateEvent);

        assertFalse(status.isPresent());

        int resourceTypeId = resourceTypeDao.getResourceTypeIdByName(event.getResourceType().name());
        Map<String, Object> result = dbHelper.getEventByExternalId(event.getResourceExternalId());
        int numberOfEvents = dbHelper.getEventsCountByExternalId(event.getResourceExternalId());
        assertThat(numberOfEvents, is(1));
        assertThat(result.get("sqs_message_id"), is(event.getSqsMessageId()));
        assertThat(result.get("resource_type_id"), is(resourceTypeId));
        assertThat(result.get("resource_external_id"), is(event.getResourceExternalId()));
        assertThat((Timestamp) result.get("event_date"), isDate(event.getEventDate()));
        assertThat(result.get("event_type"), is(event.getEventType()));
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
        assertThat(retrievedEvent.getEventType(), is(event.getEventType()));
        assertThat(retrievedEvent.getEventDate(), is(event.getEventDate()));
        assertThat(retrievedEvent.getEventData(), is(event.getEventData()));
    }
}
