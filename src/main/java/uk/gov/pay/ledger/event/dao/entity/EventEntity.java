package uk.gov.pay.ledger.event.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.service.payments.commons.api.json.MicrosecondPrecisionDateTimeSerializer;

import java.time.ZonedDateTime;
import java.util.Objects;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EventEntity {

    @JsonIgnore
    private Long id;
    @Schema(example = "fe689579-63af-40bf-a783-23de97134b5f")
    private String sqsMessageId;
    @Schema(example = "ea2d6673b23d4ff7ad88a1fd3c7ab0f6")
    private String serviceId;
    @Schema(example = "true")
    private Boolean live;
    @Schema(example = "payment")
    private ResourceType resourceType;
    @Schema(example = "58na2dr7rv7h6h53bef1l0soep")
    private String resourceExternalId;
    @Schema(example = "l40ajdf0923uojlkfjsldkjfl2", defaultValue = "null")
    private String parentResourceExternalId;
    @JsonSerialize(using = MicrosecondPrecisionDateTimeSerializer.class)
    @Schema(implementation = ZonedDateTime.class, example = "\"2022-03-30T15:09:20.241Z\"")
    private ZonedDateTime eventDate;
    @Schema(example = "PAYMENT_CREATED")
    private String eventType;
    @Schema(example = "{\"live\": false, \"moto\": false, \"amount\": 1000, \"source\": \"CARD_API\", \"language\": \"en\", \"reference\": \"my payment reference\", \"return_url\": \"https://www.payments.service.gov.uk\", \"description\": \"my payment\", \"delayed_capture\": false, \"payment_provider\": \"sandbox\", \"gateway_account_id\": \"3\", \"credential_external_id\": \"ddb294481de146c09598c8cce0461af9\", \"save_payment_instrument_to_agreement\": false}\"")
    private String eventData;
    @Schema(example = "true", defaultValue = "false")
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
