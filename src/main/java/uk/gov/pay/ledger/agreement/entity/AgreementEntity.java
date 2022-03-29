package uk.gov.pay.ledger.agreement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AgreementEntity {
    private String externalId;
    private String gatewayAccountId;
    private String serviceId;
    private String reference;
    private String description;
    private String status;
    private Boolean live;
    private ZonedDateTime createdDate;
    private Integer eventCount;
    private PaymentInstrumentEntity paymentInstrument;

    public AgreementEntity(String externalId, String gatewayAccountId, String serviceId, String reference, String description, String status, Boolean live, ZonedDateTime createdDate, Integer eventCount, PaymentInstrumentEntity paymentInstrument) {
        this.externalId = externalId;
        this.gatewayAccountId = gatewayAccountId;
        this.serviceId = serviceId;
        this.reference = reference;
        this.description = description;
        this.status = status;
        this.live = live;
        this.createdDate = createdDate;
        this.eventCount = eventCount;
        this.paymentInstrument = paymentInstrument;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getLive() {
        return live;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public PaymentInstrumentEntity getPaymentInstrument() {
        return paymentInstrument;
    }
}