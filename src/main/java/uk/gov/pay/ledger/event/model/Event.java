package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import uk.gov.pay.ledger.event.model.serializer.MicrosecondPrecisionDateTimeSerializer;

import java.time.ZonedDateTime;
import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Event {

    @JsonIgnore
    private Long id;
    private String sqsMessageId;
    private ResourceType resourceType;
    private String resourceExternalId;
    @JsonSerialize(using = MicrosecondPrecisionDateTimeSerializer.class)
    private ZonedDateTime eventDate;
    private EventType eventType;
    private String eventData;

    public Event() { }

    public Event(Long id, String sqsMessageId, ResourceType resourceType, String resourceExternalId,
                 ZonedDateTime eventDate, String eventName, String eventData) {
        this.id = id;
        this.sqsMessageId = sqsMessageId;
        this.resourceType = resourceType;
        this.resourceExternalId = resourceExternalId;
        this.eventDate = eventDate;
        this.eventType = EventType.valueOf(eventName);
        this.eventData = eventData;
    }

    public Event(String queueMessageId, ResourceType resourceType, String resourceExternalId, ZonedDateTime eventDate,
                 String eventType, String eventData) {
        this(null, queueMessageId, resourceType, resourceExternalId, eventDate, eventType, eventData);
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

    @JsonSerialize(using = ToStringSerializer.class)
    public EventType getEventType() {
        return eventType;
    }

    public String getEventData() {
        return eventData;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", sqsMessageId='" + sqsMessageId + '\'' +
                ", resourceType=" + resourceType +
                ", resourceExternalId='" + resourceExternalId + '\'' +
                ", eventDate=" + eventDate +
                ", eventType=" + eventType +
                ", eventData='" + eventData + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id) &&
                Objects.equals(sqsMessageId, event.sqsMessageId) &&
                resourceType == event.resourceType &&
                Objects.equals(resourceExternalId, event.resourceExternalId) &&
                Objects.equals(eventDate, event.eventDate) &&
                eventType == event.eventType &&
                Objects.equals(eventData, event.eventData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sqsMessageId, resourceType, resourceExternalId, eventDate, eventType, eventData);
    }
}
