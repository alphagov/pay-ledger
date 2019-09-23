package uk.gov.pay.ledger.transaction.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TransactionEntity {

    @JsonIgnore
    private Long id;
    private String gatewayAccountId;
    @JsonIgnore
    private String externalId;
    @JsonIgnore
    private String parentExternalId;
    private Long amount;
    private String reference;
    private String description;
    private TransactionState state;
    private String email;
    private String cardholderName;
    @JsonIgnore
    private ZonedDateTime createdDate;
    @JsonIgnore
    private String transactionDetails;
    @JsonIgnore
    private Integer eventCount;
    private String cardBrand;
    private String lastDigitsCardNumber;
    private String firstDigitsCardNumber;
    private Long netAmount;
    private Long totalAmount;
    private Long fee;
    private String transactionType;
    private String refundStatus;
    private Long refundAmountRefunded;
    private Long refundAmountAvailable;
    private TransactionEntity parentTransactionEntity;

    public TransactionEntity() {
    }

    public TransactionEntity(Builder builder) {
        this.id = builder.id;
        this.gatewayAccountId = builder.gatewayAccountId;
        this.externalId = builder.externalId;
        this.parentExternalId = builder.parentExternalId;
        this.amount = builder.amount;
        this.reference = builder.reference;
        this.description = builder.description;
        this.state = builder.state;
        this.email = builder.email;
        this.cardholderName = builder.cardholderName;
        this.createdDate = builder.createdDate;
        this.transactionDetails = builder.transactionDetails;
        this.eventCount = builder.eventCount;
        this.cardBrand = builder.cardBrand;
        this.lastDigitsCardNumber = builder.lastDigitsCardNumber;
        this.firstDigitsCardNumber = builder.firstDigitsCardNumber;
        this.netAmount = builder.netAmount;
        this.totalAmount = builder.totalAmount;
        this.refundStatus = builder.refundStatus;
        this.refundAmountRefunded = builder.refundAmountRefunded;
        this.refundAmountAvailable = builder.refundAmountAvailable;
        this.fee = builder.fee;
        this.transactionType = builder.transactionType;
        this.parentTransactionEntity = builder.parentTransactionEntity;
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

    public String getParentExternalId() {
        return parentExternalId;
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

    public String getEmail() {
        return email;
    }

    public String getCardholderName() {
        return cardholderName;
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

    public void setState(TransactionState state) {
        this.state = state;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setParentExternalId(String parentExternalId) {
        this.parentExternalId = parentExternalId;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Long getNetAmount() {
        return netAmount;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public Long getRefundAmountRefunded() {
        return refundAmountRefunded;
    }

    public Long getRefundAmountAvailable() {
        return refundAmountAvailable;
    }

    public Long getFee() {
        return fee;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public TransactionEntity getParentTransactionEntity() {
        return parentTransactionEntity;
    }

    public static class Builder {
        private Long fee;
        private Long id;
        private String gatewayAccountId;
        private String externalId;
        private String parentExternalId;
        private Long amount;
        private String reference;
        private String description;
        private TransactionState state;
        private String email;
        private String cardholderName;
        private ZonedDateTime createdDate;
        private String transactionDetails;
        private Integer eventCount;
        private String cardBrand;
        private String lastDigitsCardNumber;
        private String firstDigitsCardNumber;
        private Long netAmount;
        private Long totalAmount;
        private String refundStatus;
        private Long refundAmountRefunded;
        private Long refundAmountAvailable;
        private String transactionType;
        private TransactionEntity parentTransactionEntity;

        public Builder() {
        }

        public TransactionEntity build() {
            return new TransactionEntity(this);
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withGatewayAccountId(String gatewayAccountId) {
            this.gatewayAccountId = gatewayAccountId;
            return this;
        }

        public Builder withExternalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder withParentExternalId(String parentExternalId) {
            this.parentExternalId = parentExternalId;
            return this;
        }

        public Builder withAmount(Long amount) {
            this.amount = amount;
            return this;
        }

        public Builder withReference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withState(TransactionState state) {
            this.state = state;
            return this;
        }

        public Builder withEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder withCardholderName(String cardholderName) {
            this.cardholderName = cardholderName;
            return this;
        }

        public Builder withCreatedDate(ZonedDateTime createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public Builder withTransactionDetails(String transactionDetails) {
            this.transactionDetails = transactionDetails;
            return this;
        }

        public Builder withEventCount(Integer eventCount) {
            this.eventCount = eventCount;
            return this;
        }

        public Builder withCardBrand(String cardBrand) {
            this.cardBrand = cardBrand;
            return this;
        }

        public Builder withLastDigitsCardNumber(String lastDigitsCardNumber) {
            this.lastDigitsCardNumber = lastDigitsCardNumber;
            return this;
        }

        public Builder withFirstDigitsCardNumber(String firstDigitsCardNumber) {
            this.firstDigitsCardNumber = firstDigitsCardNumber;
            return this;
        }

        public Builder withNetAmount(Long netAmount) {
            this.netAmount = netAmount;
            return this;
        }

        public Builder withTotalAmount(Long totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder withRefundStatus(String refundStatus) {
            this.refundStatus = refundStatus;
            return this;
        }

        public Builder withRefundAmountRefunded(Long refundAmountRefunded) {
            this.refundAmountRefunded = refundAmountRefunded;
            return this;
        }

        public Builder withRefundAmountAvailable(Long refundAmountAvailable) {
            this.refundAmountAvailable = refundAmountAvailable;
            return this;
        }

        public Builder withFee(Long fee) {
            this.fee = fee;
            return this;
        }

        public Builder withTransactionType(String transactionType) {
            this.transactionType = transactionType;
            return this;
        }

        public Builder withParentTransactionEntity(TransactionEntity parentTransactionEntity) {
            this.parentTransactionEntity = parentTransactionEntity;
            return this;
        }

    }
}
