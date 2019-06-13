package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.ledger.event.model.serializer.MicrosecondPrecisionDateTimeSerializer;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Event {

    @JsonIgnore
    private Long id;
    private String sqsMessageId;
    private ResourceType resourceType;
    private String resourceExternalId;
    @JsonSerialize(using = MicrosecondPrecisionDateTimeSerializer.class)
    private ZonedDateTime eventDate;
    private String eventType;
    private String eventData;

    public Event() { }

    public Event(Long id, String sqsMessageId, ResourceType resourceType, String resourceExternalId,
                 ZonedDateTime eventDate, String eventType, String eventData) {
        this.id = id;
        this.sqsMessageId = sqsMessageId;
        this.resourceType = resourceType;
        this.resourceExternalId = resourceExternalId;
        this.eventDate = eventDate;
        this.eventType = eventType;
        this.eventData = eventData;
    }

    public Event(String queueMessageId, ResourceType resourceType, String resourceExternalId, ZonedDateTime eventDate,
                 String eventType, String eventData) {
        this.sqsMessageId = queueMessageId;
        this.resourceType = resourceType;
        this.resourceExternalId = resourceExternalId;
        this.eventDate = eventDate;
        this.eventType = eventType;
        this.eventData = eventData;
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
}
