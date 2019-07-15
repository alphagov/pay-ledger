package uk.gov.pay.ledger.transaction.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionEntity {

    @JsonIgnore
    private Long id;
    @JsonProperty("gateway_account_id")
    private String gatewayAccountId;
    @JsonIgnore
    private String externalId;
    private Long amount;
    private String reference;
    private String description;
    private String state;
    private String email;
    @JsonProperty("cardholder_name")
    private String cardholderName;
    @JsonProperty("external_metadata")
    private String externalMetadata;
    @JsonIgnore
    private ZonedDateTime createdDate;
    @JsonIgnore
    private String transactionDetails;
    @JsonIgnore
    private Integer eventCount;
    @JsonProperty("card_brand")
    private String cardBrand;
    @JsonProperty("last_digits_card_number")
    private String lastDigitsCardNumber;
    @JsonProperty("first_digits_card_number")
    private String firstDigitsCardNumber;


    private Long netAmount;
    private Long totalAmount;
    private ZonedDateTime settlementSubmittedTime;
    private ZonedDateTime settledTime;
    private String refundStatus;
    private Long refundAmountSubmitted;
    private Long refundAmountAvailable;

    public TransactionEntity() {}

    public TransactionEntity(Long id, String gatewayAccountId, String externalId,
                             Long amount, String reference, String description,
                             String state, String email, String cardholderName,
                             String externalMetadata, ZonedDateTime createdDate,
                             String transactionDetails, Integer eventCount, String cardBrand,
                             String lastDigitsCardNumber, String firstDigitsCardNumber,
                             Long netAmount, Long totalAmount, ZonedDateTime settlementSubmittedTime,
                             ZonedDateTime settledTime, String refundStatus, Long refundAmountSubmitted,
                             Long refundAmountAvailable) {
        this.id = id;
        this.gatewayAccountId = gatewayAccountId;
        this.externalId = externalId;
        this.amount = amount;
        this.reference = reference;
        this.description = description;
        this.state = state;
        this.email = email;
        this.cardholderName = cardholderName;
        this.externalMetadata = externalMetadata;
        this.createdDate = createdDate;
        this.transactionDetails = transactionDetails;
        this.eventCount = eventCount;
        this.cardBrand = cardBrand;
        this.lastDigitsCardNumber = lastDigitsCardNumber;
        this.firstDigitsCardNumber = firstDigitsCardNumber;
        this.netAmount = netAmount;
        this.totalAmount = totalAmount;
        this.settlementSubmittedTime = settlementSubmittedTime;
        this.settledTime = settledTime;
        this.refundStatus = refundStatus;
        this.refundAmountSubmitted = refundAmountSubmitted;
        this.refundAmountAvailable = refundAmountAvailable;
    }

    public Long getId() {
        return id;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public String getExternalId() {
        return externalId;
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

    public String getState() {
        return state;
    }

    public String getEmail() {
        return email;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public String getExternalMetadata() {
        return externalMetadata;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public String getTransactionDetails() {
        return transactionDetails;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public String getLastDigitsCardNumber() {
        return lastDigitsCardNumber;
    }

    public String getFirstDigitsCardNumber() {
        return firstDigitsCardNumber;
    }

    public void setTransactionDetails(String transactionDetails) {
        this.transactionDetails = transactionDetails;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Long getNetAmount() {
        return netAmount;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public ZonedDateTime getSettlementSubmittedTime() {
        return settlementSubmittedTime;
    }

    public ZonedDateTime getSettledTime() {
        return settledTime;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public Long getRefundAmountSubmitted() {
        return refundAmountSubmitted;
    }

    public Long getRefundAmountAvailable() {
        return refundAmountAvailable;
    }
}
