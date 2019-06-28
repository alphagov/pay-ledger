package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDetailsDigest {
    private String gatewayAccountId;
    private Long amount;
    private String reference;
    private String description;
    private String state;
    private String language;
    private String externalId;
    private String returnUrl;
    private String email;
    private String paymentProvider;
    private Boolean delayedCapture;
    private String externalMetaData;
    private String addressLine1;
    private String addressLine2;
    private String addressCity;
    private String addressPostcode;
    private String addressCounty;
    private String addressCountry;
    private String cardholderName;

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public Long getAmount() {
        return amount;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public String getLanguage() {
        return language;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public Boolean getDelayedCapture() {
        return delayedCapture;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public String getAddressPostcode() {
        return addressPostcode;
    }

    public String getAddressCounty() {
        return addressCounty;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public String getCardholderName() {
        return cardholderName;
    }
}
