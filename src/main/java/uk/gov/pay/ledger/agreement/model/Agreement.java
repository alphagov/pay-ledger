package uk.gov.pay.ledger.agreement.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.ledger.agreement.entity.AgreementEntity;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeSerializer;
import uk.gov.service.payments.commons.model.agreement.AgreementStatus;

import java.time.ZonedDateTime;
import java.util.Optional;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Agreement {
    private String externalId;
    private String serviceId;
    private String reference;
    private String description;
    private AgreementStatus status;
    private Boolean live;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private ZonedDateTime createdDate;
    private PaymentInstrument paymentInstrument;
    private String userIdentifier;

    public Agreement(String externalId, String serviceId, String reference, String description, AgreementStatus status, Boolean live, ZonedDateTime createdDate, PaymentInstrument paymentInstrument, String userIdentifier) {
        this.externalId = externalId;
        this.serviceId = serviceId;
        this.reference = reference;
        this.description = description;
        this.status = status;
        this.live = live;
        this.createdDate = createdDate;
        this.paymentInstrument = paymentInstrument;
        this.userIdentifier = userIdentifier;
    }

    public static Agreement from(AgreementEntity entity) {
        return new Agreement(
                entity.getExternalId(),
                entity.getServiceId(),
                entity.getReference(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getLive(),
                entity.getCreatedDate(),
                Optional.ofNullable(entity.getPaymentInstrument()).map(PaymentInstrument::from).orElse(null),
                entity.getUserIdentifier());
    }

    public String getExternalId() {
        return externalId;
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

    public PaymentInstrument getPaymentInstrument() {
        return paymentInstrument;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }
}