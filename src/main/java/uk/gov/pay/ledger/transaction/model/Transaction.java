package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.gson.JsonObject;
import uk.gov.pay.commons.api.json.ApiResponseDateTimeSerializer;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;
import java.util.Optional;

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
    @JsonIgnore
    private Integer eventCount;

    public Transaction() {

    }

    public Transaction(Long id, String gatewayAccountId, Long amount,
                       String reference, String description, TransactionState state,
                       String language, String externalId, String returnUrl,
                       String email, String paymentProvider, ZonedDateTime createdAt,
                       CardDetails cardDetails, Boolean delayedCapture, String externalMetaData, Integer eventCount) {
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
        this.eventCount = eventCount;
    }

    public Transaction(String gatewayAccountId, Long amount,
                       String reference, String description, TransactionState state,
                       String language, String externalId, String returnUrl,
                       String email, String paymentProvider, ZonedDateTime createdAt,
                       CardDetails cardDetails, Boolean delayedCapture, String externalMetaData, Integer eventCount) {

        this(null, gatewayAccountId, amount, reference, description, state, language, externalId, returnUrl, email,
                paymentProvider, createdAt, cardDetails, delayedCapture, externalMetaData, eventCount);
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

    public ZonedDateTime getCreatedDate() {
        return createdAt;
    }

    public CardDetails getCardDetails() {
        return cardDetails;
    }

    public Boolean getDelayedCapture() {
        return delayedCapture;
    }

    public String getExternalMetadata() {
        return externalMetaData;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public String getCardholderName() {
        return Optional.ofNullable(cardDetails)
                .map(CardDetails::getCardHolderName)
                .orElse(null);
    }

    public String getTransactionDetails() {
        JsonObject transactionDetail = new JsonObject();

        transactionDetail.addProperty("language", language);
        transactionDetail.addProperty("return_url", returnUrl);
        transactionDetail.addProperty("payment_provider", paymentProvider);
        transactionDetail.addProperty("delayed_capture", delayedCapture);
        Optional.ofNullable(getCardDetails())
                .ifPresent(cd -> {
                    Optional.ofNullable(cd.getBillingAddress())
                            .ifPresent(ba -> {
                                transactionDetail.addProperty("address_line1", ba.getAddressLine1());
                                transactionDetail.addProperty("address_line2", ba.getAddressLine2());
                                transactionDetail.addProperty("address_postcode", ba.getAddressPostCode());
                                transactionDetail.addProperty("address_city", ba.getAddressCity());
                                transactionDetail.addProperty("address_county", ba.getAddressCounty());
                                transactionDetail.addProperty("address_country", ba.getAddressCountry());
                            });

                });

        return transactionDetail.toString();
    }
}