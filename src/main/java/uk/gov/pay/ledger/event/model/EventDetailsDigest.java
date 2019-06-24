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
}
