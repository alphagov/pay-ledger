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

    public TransactionEntity() {
    }

    public TransactionEntity(Builder builder) {
        this.id = builder.id;
        this.gatewayAccountId = builder.gatewayAccountId;
        this.externalId = builder.externalId;
        this.amount = builder.amount;
        this.reference = builder.reference;
        this.description = builder.description;
        this.state = builder.state;
        this.email = builder.email;
        this.cardholderName = builder.cardholderName;
        this.externalMetadata = builder.externalMetadata;
        this.createdDate = builder.createdDate;
        this.transactionDetails = builder.transactionDetails;
        this.eventCount = builder.eventCount;
        this.cardBrand = builder.cardBrand;
        this.lastDigitsCardNumber = builder.lastDigitsCardNumber;
        this.firstDigitsCardNumber = builder.firstDigitsCardNumber;
        this.netAmount = builder.netAmount;
        this.totalAmount = builder.totalAmount;
        this.settlementSubmittedTime = builder.settlementSubmittedTime;
        this.settledTime = builder.settledTime;
        this.refundStatus = builder.refundStatus;
        this.refundAmountSubmitted = builder.refundAmountSubmitted;
        this.refundAmountAvailable = builder.refundAmountAvailable;
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

    public static class Builder {
        private Long id;
        private String gatewayAccountId;
        private String externalId;
        private Long amount;
        private String reference;
        private String description;
        private String state;
        private String email;
        private String cardholderName;
        private String externalMetadata;
        private ZonedDateTime createdDate;
        private String transactionDetails;
        private Integer eventCount;
        private String cardBrand;
        private String lastDigitsCardNumber;
        private String firstDigitsCardNumber;
        private Long netAmount;
        private Long totalAmount;
        private ZonedDateTime settlementSubmittedTime;
        private ZonedDateTime settledTime;
        private String refundStatus;
        private Long refundAmountSubmitted;
        private Long refundAmountAvailable;

        public Builder() {
        }

        public TransactionEntity build() {
            return new TransactionEntity(this);
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder gatewayAccountId(String gatewayAccountId) {
            this.gatewayAccountId = gatewayAccountId;
            return this;
        }

        public Builder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder amount(Long amount) {
            this.amount = amount;
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder cardholderName(String cardholderName) {
            this.cardholderName = cardholderName;
            return this;
        }

        public Builder externalMetadata(String externalMetadata) {
            this.externalMetadata = externalMetadata;
            return this;
        }

        public Builder createdDate(ZonedDateTime createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public Builder transactionDetails(String transactionDetails) {
            this.transactionDetails = transactionDetails;
            return this;
        }

        public Builder eventCount(Integer eventCount) {
            this.eventCount = eventCount;
            return this;
        }

        public Builder cardBrand(String cardBrand) {
            this.cardBrand = cardBrand;
            return this;
        }

        public Builder lastDigitsCardNumber(String lastDigitsCardNumber) {
            this.lastDigitsCardNumber = lastDigitsCardNumber;
            return this;
        }

        public Builder firstDigitsCardNumber(String firstDigitsCardNumber) {
            this.firstDigitsCardNumber = firstDigitsCardNumber;
            return this;
        }

        public Builder netAmount(Long netAmount) {
            this.netAmount = netAmount;
            return this;
        }

        public Builder totalAmount(Long totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder settlementSubmittedTime(ZonedDateTime settlementSubmittedTime) {
            this.settlementSubmittedTime = settlementSubmittedTime;
            return this;
        }

        public Builder settledTime(ZonedDateTime settledTime) {
            this.settledTime = settledTime;
            return this;
        }

        public Builder refundStatus(String refundStatus) {
            this.refundStatus = refundStatus;
            return this;
        }

        public Builder refundAmountSubmitted(Long refundAmountSubmitted) {
            this.refundAmountSubmitted = refundAmountSubmitted;
            return this;
        }

        public Builder refundAmountAvailable(Long refundAmountAvailable) {
            this.refundAmountAvailable = refundAmountAvailable;
            return this;
        }
    }
}
