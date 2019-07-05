package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import uk.gov.pay.commons.api.json.ApiResponseDateTimeSerializer;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Payment extends Transaction{

    private String reference;
    private String description;
    @JsonSerialize(using = ToStringSerializer.class)
    private TransactionState state;
    private String language;
    private String returnUrl;
    private String email;
    private String paymentProvider;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private ZonedDateTime createdDate;
    private CardDetails cardDetails;
    private Boolean delayedCapture;
    private String externalMetaData;
    @JsonIgnore
    private Integer eventCount;
    private String gatewayTransactionId;

    public Payment() {

    }

    public Payment(Long id, String gatewayAccountId, Long amount,
                   String reference, String description, TransactionState state,
                   String language, String externalId, String returnUrl,
                   String email, String paymentProvider, ZonedDateTime createdDate,
                   CardDetails cardDetails, Boolean delayedCapture, String externalMetaData,
                   Integer eventCount, String gatewayTransactionId) {
        super(id, gatewayAccountId, amount, externalId);
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
        this.createdDate = createdDate;
        this.cardDetails = cardDetails;
        this.delayedCapture = delayedCapture;
        this.externalMetaData = externalMetaData;
        this.eventCount = eventCount;
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public Payment(String gatewayAccountId, Long amount,
                   String reference, String description, TransactionState state,
                   String language, String externalId, String returnUrl,
                   String email, String paymentProvider, ZonedDateTime createdDate,
                   CardDetails cardDetails, Boolean delayedCapture, String externalMetaData, Integer eventCount) {

        this(null, gatewayAccountId, amount, reference, description, state, language, externalId, returnUrl, email,
                paymentProvider, createdDate, cardDetails, delayedCapture, externalMetaData, eventCount, null);
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

    @Override
    @JsonProperty("charge_id")
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
        return createdDate;
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

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public static Payment fromTransactionEntity(TransactionEntity entity) {
        JsonObject transactionDetail = new JsonParser().parse(entity.getTransactionDetails()).getAsJsonObject();
        Address billingAddress = new Address(
                    safeGetAsString(transactionDetail, "address_line1"),
                    safeGetAsString(transactionDetail, "address_line2"),
                    safeGetAsString(transactionDetail, "address_postcode"),
                    safeGetAsString(transactionDetail, "address_city"),
                    safeGetAsString(transactionDetail, "address_county"),
                    safeGetAsString(transactionDetail, "address_country")
            );

        CardDetails cardDetails = new CardDetails(entity.getCardholderName(), billingAddress, entity.getCardBrand(),
                entity.getLastDigitsCardNumber(), entity.getFirstDigitsCardNumber(),
                safeGetAsString(transactionDetail, "card_expiry_date"));

        return new Payment(entity.getGatewayAccountId(), entity.getAmount(), entity.getReference(), entity.getDescription(),
                TransactionState.from(entity.getState()), safeGetAsString(transactionDetail, "language"),
                entity.getExternalId(), safeGetAsString(transactionDetail, "return_url"), entity.getEmail(),
                safeGetAsString(transactionDetail, "payment_provider"), entity.getCreatedDate(),
                cardDetails, Boolean.valueOf(safeGetAsString(transactionDetail, "delayed_capture")),
                entity.getExternalMetadata(), entity.getEventCount());
    }
}