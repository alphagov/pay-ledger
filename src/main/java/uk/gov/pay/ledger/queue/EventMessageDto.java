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

import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EventMessageDto {

    private String serviceId;

    private Boolean live;

    @NotNull(message = "Field [timestamp] cannot be null")
    @JsonDeserialize(using = MicrosecondPrecisionDateTimeDeserializer.class)
    private ZonedDateTime timestamp;

    @NotNull(message = "Field [resource_external_id] cannot be null")
    @JsonProperty("resource_external_id")
    private String externalId;

    @JsonProperty("parent_resource_external_id")
    private String parentExternalId;

    @NotNull(message = "Field [event_type] cannot be null")
    public String eventType;

    @NotNull(message = "Field [resource_type] cannot be null")
    public ResourceType resourceType;

    @NotNull(message = "Field [event_details] cannot be null")
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
