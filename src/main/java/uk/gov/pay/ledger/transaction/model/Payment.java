package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import uk.gov.pay.commons.api.json.ApiResponseDateTimeSerializer;
import uk.gov.pay.commons.model.Source;
import uk.gov.pay.ledger.transaction.search.model.PaymentSettlementSummary;
import uk.gov.pay.ledger.transaction.search.model.RefundSummary;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;
import java.util.Map;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Payment extends Transaction {

    private Boolean moto;
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
    private Map<String, Object> externalMetaData;
    @JsonIgnore
    private Integer eventCount;
    private String gatewayTransactionId;
    private Long corporateCardSurcharge;
    private Long fee;
    private Long netAmount;
    private Long totalAmount;
    private RefundSummary refundSummary;
    private PaymentSettlementSummary settlementSummary;
    private Boolean live;
    private Source source;
    private String walletType;

    public Payment() {

    }

    public Payment(Builder builder) {
        super(builder.id, builder.gatewayAccountId, builder.amount, builder.externalId, builder.gatewayPayoutId);
        this.corporateCardSurcharge = builder.corporateCardSurcharge;
        this.fee = builder.fee;
        this.netAmount = builder.netAmount;
        this.totalAmount = builder.totalAmount;
        this.refundSummary = builder.refundSummary;
        this.settlementSummary = builder.settlementSummary;
        this.reference = builder.reference;
        this.description = builder.description;
        this.state = builder.state;
        this.language = builder.language;
        this.returnUrl = builder.returnUrl;
        this.email = builder.email;
        this.paymentProvider = builder.paymentProvider;
        this.createdDate = builder.createdDate;
        this.cardDetails = builder.cardDetails;
        this.delayedCapture = builder.delayedCapture;
        this.externalMetaData = builder.externalMetaData;
        this.eventCount = builder.eventCount;
        this.gatewayTransactionId = builder.gatewayTransactionId;
        this.moto = builder.moto;
        this.live = builder.live;
        this.source = builder.source;
        this.walletType = builder.walletType;
        this.eventCount = builder.eventCount;
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.PAYMENT;
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

    public Boolean getMoto() {
        return moto;
    }

    public Map<String, Object> getExternalMetadata() {
        return externalMetaData;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public Long getCorporateCardSurcharge() {
        return corporateCardSurcharge;
    }

    public Long getFee() {
        return fee;
    }

    public Long getNetAmount() {
        return netAmount;
    }

    public RefundSummary getRefundSummary() {
        return refundSummary;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public PaymentSettlementSummary getSettlementSummary() {
        return settlementSummary;
    }

    public Boolean isLive() {
        return live;
    }

    public Source getSource() {
        return source;
    }

    public String getWalletType() {
        return walletType;
    }

    public static class Builder {
        private Long id;
        private Boolean moto;
        private String reference;
        private String description;
        private TransactionState state;
        private String language;
        private String returnUrl;
        private String email;
        private String paymentProvider;
        private ZonedDateTime createdDate;
        private CardDetails cardDetails;
        private Boolean delayedCapture;
        private Map<String, Object> externalMetaData;
        private Integer eventCount;
        private String gatewayTransactionId;
        private Long corporateCardSurcharge;
        private Long fee;
        private Long netAmount;
        private Long totalAmount;
        private Long amount;
        private RefundSummary refundSummary;
        private PaymentSettlementSummary settlementSummary;
        private Boolean live;
        private Source source;
        private String walletType;
        private String gatewayAccountId;
        private String externalId;
        private String gatewayPayoutId;

        public Builder() {

        }

        public Payment build() {
            return new Payment(this);
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withGatewayAccountId(String gatewayAccountId) {
            this.gatewayAccountId = gatewayAccountId;
            return this;
        }

        public Builder withAmount(Long amount) {
            this.amount = amount;
            return this;
        }

        public Builder withTotalAmount(Long totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder withCorporateCardSurcharge(Long corporateCardSurcharge) {
            this.corporateCardSurcharge = corporateCardSurcharge;
            return this;
        }

        public Builder withFee(Long fee) {
            this.fee = fee;
            return this;
        }

        public Builder withNetAmount(Long netAmount) {
            this.netAmount = netAmount;
            return this;
        }

        public Builder withState(TransactionState state) {
            this.state = state;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withReference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder withLanguage(String language) {
            this.language = language;
            return this;
        }

        public Builder withExternalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder withReturnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
            return this;
        }

        public Builder withEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder withPaymentProvider(String paymentProvider) {
            this.paymentProvider = paymentProvider;
            return this;
        }

        public Builder withCreatedDate(ZonedDateTime createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public Builder withCardDetails(CardDetails cardDetails) {
            this.cardDetails = cardDetails;
            return this;
        }

        public Builder withDelayedCapture(Boolean delayedCapture) {
            this.delayedCapture = delayedCapture;
            return this;
        }

        public Builder withGatewayTransactionId(String gatewayTransactionId) {
            this.gatewayTransactionId = gatewayTransactionId;
            return this;
        }

        public Builder withRefundSummary(RefundSummary refundSummary) {
            this.refundSummary = refundSummary;
            return this;
        }

        public Builder withSettlementSummary(PaymentSettlementSummary paymentSettlementSummary) {
            this.settlementSummary = paymentSettlementSummary;
            return this;
        }

        public Builder withExternalMetadata(Map<String, Object> externalMetaData) {
            this.externalMetaData = externalMetaData;
            return this;
        }

        public Builder withMoto(Boolean moto) {
            this.moto = moto;
            return this;
        }

        public Builder withLive(Boolean live) {
            this.live = live;
            return this;
        }

        public Builder withSource(Source source) {
            this.source = source;
            return this;
        }

        public Builder withWalletType(String walletType) {
            this.walletType = walletType;
            return this;
        }

        public Builder withGatewayPayoutId(String gatewayPayoutId) {
            this.gatewayPayoutId = gatewayPayoutId;
            return this;
        }

        public Builder withEventCount(Integer eventCount) {
            this.eventCount = eventCount;
            return this;
        }
    }
}