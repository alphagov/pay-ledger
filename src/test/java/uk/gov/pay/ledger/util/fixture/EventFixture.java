package uk.gov.pay.ledger.util.fixture;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class EventFixture implements DbFixture<EventFixture, Event> {
    private Long id = RandomUtils.nextLong(1, 99999);
    private String sqsMessageId = RandomStringUtils.randomAlphanumeric(50);
    private ResourceType resourceType = ResourceType.PAYMENT;
    private String resourceExternalId = RandomStringUtils.randomAlphanumeric(20);
    private String parentResourceExternalId = StringUtils.EMPTY;
    private ZonedDateTime eventDate = ZonedDateTime.now(ZoneOffset.UTC);
    private String eventType = "PAYMENT_CREATED";
    private String eventData = "{\"event_data\": \"event data\"}";

    private EventFixture() {
    }

    public static EventFixture anEventFixture() {
        return new EventFixture();
    }

    public EventFixture withId(Long id) {
        this.id = id;
        return this;
    }

    public EventFixture withSQSMessageId(String sqsMessageId) {
        this.sqsMessageId = sqsMessageId;
        return this;
    }

    public EventFixture withResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public EventFixture withResourceExternalId(String resourceExternalId) {
        this.resourceExternalId = resourceExternalId;
        return this;
    }

    public EventFixture withParentResourceExternalId(String parentResourceExternalId) {
        this.parentResourceExternalId = parentResourceExternalId;
        return this;
    }

    public EventFixture withEventDate(ZonedDateTime eventDate) {
        this.eventDate = eventDate;
        return this;
    }

    public EventFixture withEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public EventFixture withEventData(String eventData) {
        this.eventData = eventData;
        return this;
    }

    @Override
    public EventFixture insert(Jdbi jdbi) {
        jdbi.withHandle(h ->
                h.execute(
                        "INSERT INTO" +
                                "    event(\n" +
                                "        id,\n" +
                                "        sqs_message_id, \n" +
                                "        resource_type_id, \n" +
                                "        resource_external_id,\n" +
                                "        parent_resource_external_id,\n" +
                                "        event_date,\n" +
                                "        event_type,\n" +
                                "        event_data\n" +
                                "    )\n" +
                                "   VALUES(?, ?, (SELECT rt.id FROM resource_type rt WHERE upper(rt.name) = ?), ?, ?, ?, ?, CAST(? as jsonb))\n",
                        id,
                        sqsMessageId,
                        resourceType,
                        resourceExternalId,
                        parentResourceExternalId,
                        eventDate,
                        eventType,
                        eventData
                 )
        );
        return this;
    }

    @Override
    public Event toEntity() {
        return new Event(id, sqsMessageId, resourceType, resourceExternalId, parentResourceExternalId, eventDate, eventType, eventData);
    }

    public Long getId() {
        return id;
    }

    public String getSqsMessageId() {
        return sqsMessageId;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getResourceExternalId() {
        return resourceExternalId;
    }

    public ZonedDateTime getEventDate() {
        return eventDate;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventData() {
        return eventData;
    }

    public EventFixture from(Event event) {
        id = event.getId();
        sqsMessageId = event.getSqsMessageId();
        resourceType = event.getResourceType();
        resourceExternalId = event.getResourceExternalId();
        parentResourceExternalId = event.getParentResourceExternalId();
        eventDate = event.getEventDate();
        eventType = event.getEventType().toString();
        eventData = event.getEventData();
        return this;
    }
}
