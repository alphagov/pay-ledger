package uk.gov.pay.ledger.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.dao.ResourceTypeDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.rules.AppWithPostgresRule;
import uk.gov.pay.ledger.utils.DatabaseTestHelper;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.ledger.utils.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.utils.ZonedDateTimeTimestampMatcher.isDate;
import static uk.gov.pay.ledger.utils.fixtures.EventFixture.anEventFixture;

@Ignore
public class EventDaoITest {

    @ClassRule
    public static AppWithPostgresRule rule = new AppWithPostgresRule();

    private static final ZonedDateTime CREATED_AT = ZonedDateTime.parse("2019-06-07T08:46:01.123456Z");
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void shouldInsertEvent() throws IOException {
        Event event = anEventFixture()
                .withEventDate(CREATED_AT)
                .toEntity();
        EventDao eventDao = rule.getJdbi().onDemand(EventDao.class);
        eventDao.insertEventWithResourceTypeId(event);
        ResourceTypeDao resourceTypeDao = rule.getJdbi().onDemand(ResourceTypeDao.class);
        int resourceTypeId = resourceTypeDao.getResourceTypeIdByName(event.getResourceType().name());
        DatabaseTestHelper dbHelper = aDatabaseTestHelper(rule.getJdbi());
        Map<String, Object> result = dbHelper.getEventByExternalId(event.getResourceExternalId());
        assertThat(result.get("sqs_message_id"), is(event.getSqsMessageId()));
        assertThat(result.get("resource_type_id"), is(resourceTypeId));
        assertThat(result.get("resource_external_id"), is(event.getResourceExternalId()));
        assertThat((Timestamp) result.get("event_date"), isDate(CREATED_AT));
        assertThat(result.get("event_type"), is(event.getEventType()));
        assertThat(objectMapper.readTree(result.get("event_data").toString()), is(objectMapper.readTree(event.getEventData())));
    }

    @Test
    public void shouldFindEvent() {
        Event event = anEventFixture()
                .insert(rule.getJdbi())
                .toEntity();
        EventDao eventDao = rule.getJdbi().onDemand(EventDao.class);
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
