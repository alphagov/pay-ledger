package uk.gov.pay.ledger.agreement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
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

    public AgreementEntity() {

    }

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

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setLive(Boolean live) {
        this.live = live;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPaymentInstrument(PaymentInstrumentEntity paymentInstrument) {
        this.paymentInstrument = paymentInstrument;
    }
}