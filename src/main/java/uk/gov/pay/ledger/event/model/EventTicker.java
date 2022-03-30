package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.service.payments.commons.api.json.MicrosecondPrecisionDateTimeSerializer;

import java.time.ZonedDateTime;
import java.util.Objects;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EventTicker {

    @JsonIgnore
    private Long id;
    @Schema(example = "9np5pocnotgkpp029d5kdfau5f")
    private String resourceExternalId;
    @Schema(example = "payment")
    private ResourceType resourceType;
    @JsonSerialize(using = MicrosecondPrecisionDateTimeSerializer.class)
    @Schema(implementation = ZonedDateTime.class, example = "\"2022-03-30T15:09:20.241Z\"")
    private ZonedDateTime eventDate;
    @Schema(example = "PAYMENT_CREATED")
    private String eventType;
    @Schema(example = "visa")
    private String cardBrand;
    @Schema(example = "PAYMENT")
    private String transactionType;
    @Schema(example = "sandbox")
    private String paymentProvider;
    @Schema(example = "1")
    private String gatewayAccountId;
    @Schema(example = "100")
    private Long amount;

    public EventTicker() { }

    public EventTicker(Long id, ResourceType resourceType, String resourceExternalId,
                       ZonedDateTime eventDate, String eventType, String cardBrand, String transactionType, String paymentProvider, String gatewayAccountId, Long amount) {
        this.id = id;
        this.resourceType = resourceType;
        this.resourceExternalId = resourceExternalId;
        this.eventDate = eventDate;
        this.eventType = eventType;
        this.cardBrand = cardBrand;
        this.transactionType = transactionType;
        this.paymentProvider = paymentProvider;
        this.gatewayAccountId = gatewayAccountId;
        this.amount = amount;
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

    public String getCardBrand() {
        return cardBrand;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public Long getAmount() {
        return amount;
    }
}
