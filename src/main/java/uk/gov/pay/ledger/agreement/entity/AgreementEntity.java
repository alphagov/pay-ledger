package uk.gov.pay.ledger.agreement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.service.payments.commons.api.json.MicrosecondPrecisionDateTimeDeserializer;
import uk.gov.service.payments.commons.api.json.MicrosecondPrecisionDateTimeSerializer;
import uk.gov.service.payments.commons.model.agreement.AgreementStatus;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AgreementEntity {
    private String externalId;
    private String gatewayAccountId;
    private String serviceId;
    private String reference;
    private String description;
    private AgreementStatus status;
    private Boolean live;
    private ZonedDateTime createdDate;
    private Integer eventCount;
    private PaymentInstrumentEntity paymentInstrument;
    private String userIdentifier;
    @JsonDeserialize(using = MicrosecondPrecisionDateTimeDeserializer.class)
    @JsonSerialize(using = MicrosecondPrecisionDateTimeSerializer.class)
    private ZonedDateTime cancelledDate;

    private String cancelledByUserEmail;

    public AgreementEntity() {

    }

    public AgreementEntity(String externalId, String gatewayAccountId, String serviceId, String reference,
                           String description, AgreementStatus status, Boolean live, ZonedDateTime createdDate,
                           Integer eventCount, PaymentInstrumentEntity paymentInstrument, String userIdentifier,
                           ZonedDateTime cancelledDate, String cancelledByUserEmail) {
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
        this.userIdentifier = userIdentifier;
        this.cancelledDate = cancelledDate;
        this.cancelledByUserEmail = cancelledByUserEmail;
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

    public AgreementStatus getStatus() {
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

    public void setStatus(AgreementStatus status) {
        this.status = status;
    }

    public void setPaymentInstrument(PaymentInstrumentEntity paymentInstrument) {
        this.paymentInstrument = paymentInstrument;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public ZonedDateTime getCancelledDate() {
        return cancelledDate;
    }

    public String getCancelledByUserEmail() {
        return cancelledByUserEmail;
    }

    public void setCancelledDate(ZonedDateTime cancelledDate) {
        this.cancelledDate = cancelledDate;
    }

    public void setCancelledByUserEmail(String cancelledByUserEmail) {
        this.cancelledByUserEmail = cancelledByUserEmail;
    }
}
