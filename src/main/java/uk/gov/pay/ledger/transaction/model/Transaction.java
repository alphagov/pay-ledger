package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.commons.api.json.ApiResponseDateTimeSerializer;

import java.time.ZonedDateTime;

public class Transaction {

    @JsonIgnore
    private Long id;
    @JsonIgnore
    private String gatewayAccountId;
    @JsonProperty
    private Long amount;
    @JsonProperty
    private String reference;
    @JsonProperty
    private String description;
    @JsonProperty("state")
    private String status;
    @JsonProperty
    private String language;
    @JsonProperty("charge_id")
    private String externalId;
    @JsonProperty("return_url")
    private String returnUrl;
    @JsonProperty
    private String email;
    @JsonProperty("payment_provider")
    private String paymentProvider;
    @JsonProperty("created_date")
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private ZonedDateTime createdAt;
    @JsonProperty("card_details")
    private CardDetails cardDetails;
    @JsonProperty("delayed_capture")
    private Boolean delayedCapture;
    @JsonProperty("external_metadata")
    private String externalMetaData;

    public Transaction(){

    }

    public Transaction(Long id, String gatewayAccountId, Long amount,
                       String reference, String description, String status,
                       String language, String externalId, String returnUrl,
                       String email, String paymentProvider, ZonedDateTime createdAt,
                       CardDetails cardDetails, Boolean delayedCapture, String externalMetaData) {
        this.id = id;
        this.gatewayAccountId = gatewayAccountId;
        this.amount = amount;
        this.reference = reference;
        this.description = description;
        this.status = status;
        this.language = language;
        this.externalId = externalId;
        this.returnUrl = returnUrl;
        this.email = email;
        this.paymentProvider = paymentProvider;
        this.createdAt = createdAt;
        this.cardDetails = cardDetails;
        this.delayedCapture = delayedCapture;
        this.externalMetaData = externalMetaData;
    }


    public Long getId() {
        return id;
    }

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

    public String getStatus() {
        return status;
    }

    public String getLanguage() {
        return language;
    }

    public String getExternalId() {
        return externalId;
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

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public CardDetails getCardDetails() {
        return cardDetails;
    }

    public Boolean getDelayedCapture() {
        return delayedCapture;
    }

    public String getExternalMetaData() {
        return externalMetaData;
    }
}
