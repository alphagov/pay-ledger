package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import uk.gov.pay.commons.api.json.ApiResponseDateTimeSerializer;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Transaction {

    @JsonIgnore
    private Long id;
    private String gatewayAccountId;
    private Long amount;
    private String reference;
    private String description;
    @JsonSerialize(using = ToStringSerializer.class)
    private TransactionState state;
    private String language;
    private String externalId;
    private String returnUrl;
    private String email;
    private String paymentProvider;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private ZonedDateTime createdAt;
    private CardDetails cardDetails;
    private Boolean delayedCapture;
    private String externalMetaData;

    public Transaction() {

    }

    public Transaction(Long id, String gatewayAccountId, Long amount,
                       String reference, String description, TransactionState state,
                       String language, String externalId, String returnUrl,
                       String email, String paymentProvider, ZonedDateTime createdAt,
                       CardDetails cardDetails, Boolean delayedCapture, String externalMetaData) {
        this.id = id;
        this.gatewayAccountId = gatewayAccountId;
        this.amount = amount;
        this.reference = reference;
        this.description = description;
        this.state = state;
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

    public Transaction(String gatewayAccountId, Long amount,
                       String reference, String description, TransactionState state,
                       String language, String externalId, String returnUrl,
                       String email, String paymentProvider, ZonedDateTime createdAt,
                       CardDetails cardDetails, Boolean delayedCapture, String externalMetaData) {

        this(null, gatewayAccountId, amount, reference, description, state, language, externalId, returnUrl, email,
                paymentProvider, createdAt, cardDetails, delayedCapture, externalMetaData);
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

    public TransactionState getState() {
        return state;
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