package uk.gov.pay.ledger.queue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.service.payments.commons.api.json.MicrosecondPrecisionDateTimeDeserializer;
import uk.gov.pay.ledger.event.model.ResourceType;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventMessageDto {

    @JsonDeserialize(using = MicrosecondPrecisionDateTimeDeserializer.class)
    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;

    @JsonProperty("resource_external_id")
    private String externalId;

    @JsonProperty("parent_resource_external_id")
    private String parentExternalId;

    @JsonProperty("event_type")
    public String eventType;

    @JsonProperty("resource_type")
    public ResourceType resourceType;

    @JsonProperty("event_details")
    private JsonNode eventData;

    @JsonProperty("reproject_domain_object")
    private boolean reprojectDomainObject;

    public ResourceType getResourceType() {
        return resourceType;
    }

    public ZonedDateTime getEventDate() {
        return timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventData() {
        return eventData.toString();
    }

    public String getExternalId() {
        return externalId;
    }

    public String getParentExternalId() {
        return parentExternalId;
    }

    public boolean isReprojectDomainObject() {
        return reprojectDomainObject;
    }
}
