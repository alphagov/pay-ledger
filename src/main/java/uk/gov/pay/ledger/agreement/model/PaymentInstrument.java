package uk.gov.pay.ledger.agreement.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.ledger.agreement.entity.PaymentInstrumentEntity;
import uk.gov.pay.ledger.transaction.model.Address;
import uk.gov.pay.ledger.transaction.model.CardDetails;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeSerializer;
import uk.gov.service.payments.commons.model.agreement.PaymentInstrumentType;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class PaymentInstrument {
    private String externalId;
    private String agreementExternalId;
    private CardDetails cardDetails;
    private PaymentInstrumentType type;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private ZonedDateTime createdDate;

    public PaymentInstrument(String externalId, String agreementExternalId, CardDetails cardDetails, PaymentInstrumentType type, ZonedDateTime createdDate) {
        this.externalId = externalId;
        this.agreementExternalId = agreementExternalId;
        this.cardDetails = cardDetails;
        this.type = type;
        this.createdDate = createdDate;
    }

    public static PaymentInstrument from(PaymentInstrumentEntity entity) {
        var billingAddress = Address.from(
                entity.getAddressLine1(),
                entity.getAddressLine2(),
                entity.getAddressPostcode(),
                entity.getAddressCity(),
                entity.getAddressCounty(),
                entity.getAddressCountry()
        );
        var cardDetails = CardDetails.from(
                entity.getCardholderName(),
                billingAddress,
                entity.getCardBrand(),
                entity.getLastDigitsCardNumber(),
                null,
                entity.getExpiryDate(),
                null
        );
        return new PaymentInstrument(
                entity.getExternalId(),
                entity.getAgreementExternalId(),
                cardDetails,
                entity.getType(),
                entity.getCreatedDate()
        );
    }

    public String getExternalId() {
        return externalId;
    }

    public String getAgreementExternalId() {
        return agreementExternalId;
    }

    public CardDetails getCardDetails() {
        return cardDetails;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public PaymentInstrumentType getType() {
        return type;
    }
}