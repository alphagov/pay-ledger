package uk.gov.pay.ledger.queue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.service.payments.commons.api.json.MicrosecondPrecisionDateTimeDeserializer;
import uk.gov.service.payments.commons.api.json.MicrosecondPrecisionDateTimeSerializer;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EventMessageDto {

    private String serviceId;

    private Boolean live;

    @JsonDeserialize(using = MicrosecondPrecisionDateTimeDeserializer.class)
    private ZonedDateTime timestamp;

    @JsonProperty("resource_external_id")
    private String externalId;

    @JsonProperty("parent_resource_external_id")
    private String parentExternalId;

    public String eventType;

    public ResourceType resourceType;

    @JsonProperty("event_details")
    private JsonNode eventData;

    private boolean reprojectDomainObject;

    public String getServiceId() {
        return serviceId;
    }

    public Boolean isLive() {
        return live;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    @JsonSerialize(using = MicrosecondPrecisionDateTimeSerializer.class)
    public ZonedDateTime getTimestamp() {
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