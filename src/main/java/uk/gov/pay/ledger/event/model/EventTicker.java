package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.ledger.event.model.serializer.MicrosecondPrecisionDateTimeSerializer;

import java.time.ZonedDateTime;
import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class EventTicker {

    @JsonIgnore
    private Long id;
    private ResourceType resourceType;
    private String resourceExternalId;
    @JsonSerialize(using = MicrosecondPrecisionDateTimeSerializer.class)
    private ZonedDateTime eventDate;
    private String eventType;

    public EventTicker() { }

    public EventTicker(Long id, ResourceType resourceType, String resourceExternalId,
                       ZonedDateTime eventDate, String eventType) {
        this.id = id;
        this.resourceType = resourceType;
        this.resourceExternalId = resourceExternalId;
        this.eventDate = eventDate;
        this.eventType = eventType;
    }

    public EventTicker(ResourceType resourceType, String resourceExternalId, ZonedDateTime eventDate,
                       String eventType) {
        this(null, resourceType, resourceExternalId, eventDate, eventType);
    }

    public Long getId() {
        return id;
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

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", resourceType=" + resourceType +
                ", resourceExternalId='" + resourceExternalId + '\'' +
                ", eventDate=" + eventDate +
                ", eventType=" + eventType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventTicker event = (EventTicker) o;
        return Objects.equals(id, event.id) &&
                resourceType == event.resourceType &&
                Objects.equals(resourceExternalId, event.resourceExternalId) &&
                Objects.equals(eventDate, event.eventDate) &&
                Objects.equals(eventType, event.eventType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, resourceType, resourceExternalId, eventDate, eventType);
    }
}
