package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.dao.entity.EventEntity;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeSerializer;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Event {

    private static final Logger logger = LoggerFactory.getLogger(Event.class);
    
    private final String resourceExternalId;
    @Schema(example = "AGREEMENT")
    private final ResourceType resourceType;
    @Schema(example = "CREATED")
    private final String eventType;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    @Schema(example = "\"2022-03-29T16:58:49.298Z\"")
    private final ZonedDateTime timestamp;
    private final Boolean live;
    private final String serviceId;
    private final Map<String, Object> data;

    public Event(String resourceExternalId,
                 ResourceType resourceType,
                 String eventType,
                 ZonedDateTime timestamp,
                 Boolean live,
                 String serviceId,
                 Map<String, Object> data) {
        this.resourceExternalId = resourceExternalId;
        this.resourceType = resourceType;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.live = live;
        this.serviceId = serviceId;
        this.data = data;
    }

    public static Event from(EventEntity event, ObjectMapper objectMapper) {
        try {
            return new Event(
                    event.getResourceExternalId(),
                    event.getResourceType(),
                    event.getEventType(),
                    event.getEventDate(),
                    event.getLive(),
                    event.getServiceId(),
                    objectMapper.readValue(event.getEventData(), new TypeReference<>() {
                    })
            );
        }
        catch (IOException e) {
            logger.error("Error parsing event data [Resource external ID - {}] [errorMessage={}]",
                    event.getResourceExternalId(),
                    e.getMessage());
            return null;
        }
    }

    public String getResourceExternalId() {
        return resourceExternalId;
    }

    public String getResourceType() {
        return resourceType.toString().toUpperCase();
    }

    public String getEventType() {
        return eventType;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Boolean getLive() {
        return live;
    }

    public String getServiceId() {
        return serviceId;
    }
}
