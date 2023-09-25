package uk.gov.pay.ledger.event.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.ledger.event.model.ResourceType;

import java.time.ZonedDateTime;
import java.util.Objects;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EventEntity {

    @JsonIgnore
    private Long id;
    private String sqsMessageId;
    private String serviceId;
    private Boolean live;
    private ResourceType resourceType;
    private String resourceExternalId;
    private String parentResourceExternalId;
    private ZonedDateTime eventDate;
    private String eventType;
    private String eventData;
    private boolean reprojectDomainObject;

    public EventEntity() {
    }

    public EventEntity(Long id,
                       String sqsMessageId,
                       String serviceId,
                       Boolean live,
                       ResourceType resourceType,
                       String resourceExternalId,
                       String parentResourceExternalId,
                       ZonedDateTime eventDate,
                       String eventType,
                       String eventData,
                       boolean reprojectDomainObject) {
        this.id = id;
        this.sqsMessageId = sqsMessageId;
        this.serviceId = serviceId;
        this.live = live;
        this.resourceType = resourceType;
        this.resourceExternalId = resourceExternalId;
        this.parentResourceExternalId = parentResourceExternalId;
        this.eventDate = eventDate;
        this.eventType = eventType;
        this.eventData = eventData;
        this.reprojectDomainObject = reprojectDomainObject;
    }

    public EventEntity(String sqsMessageId,
                       String serviceId,
                       Boolean live,
                       ResourceType resourceType,
                       String resourceExternalId,
                       String parentResourceExternalId,
                       ZonedDateTime eventDate,
                       String eventType,
                       String eventData,
                       boolean reprojectDomainObject) {
        this(null, sqsMessageId, serviceId, live, resourceType, resourceExternalId, parentResourceExternalId, eventDate, eventType,
                eventData, reprojectDomainObject);
    }

    public Long getId() {
        return id;
    }

    public String getSqsMessageId() {
        return sqsMessageId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public Boolean getLive() {
        return live;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getResourceExternalId() {
        return resourceExternalId;
    }

    public String getParentResourceExternalId() {
        return parentResourceExternalId;
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

    public boolean isReprojectDomainObject() {
        return reprojectDomainObject;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", sqsMessageId='" + sqsMessageId + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", isLive=" + live +
                ", resourceType=" + resourceType +
                ", resourceExternalId='" + resourceExternalId + '\'' +
                ", parentResourceExternalId='" + parentResourceExternalId + '\'' +
                ", eventDate=" + eventDate +
                ", eventType=" + eventType +
                ", eventData='" + eventData + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventEntity event = (EventEntity) o;
        return live == event.live &&
                reprojectDomainObject == event.reprojectDomainObject &&
                Objects.equals(id, event.id) &&
                Objects.equals(sqsMessageId, event.sqsMessageId) &&
                Objects.equals(serviceId, event.serviceId) &&
                resourceType == event.resourceType &&
                Objects.equals(resourceExternalId, event.resourceExternalId) &&
                Objects.equals(parentResourceExternalId, event.parentResourceExternalId) &&
                Objects.equals(eventDate, event.eventDate) &&
                Objects.equals(eventType, event.eventType) &&
                Objects.equals(eventData, event.eventData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sqsMessageId, serviceId, live, resourceType, resourceExternalId, parentResourceExternalId, eventDate, eventType, eventData, reprojectDomainObject);
    }
}
