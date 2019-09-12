package uk.gov.pay.ledger.transaction.search.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.commons.api.json.ApiResponseDateTimeSerializer;
import uk.gov.pay.ledger.transaction.model.CardDetails;
import uk.gov.pay.ledger.transaction.model.Payment;
import uk.gov.pay.ledger.transaction.model.Refund;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.ExternalTransactionState;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TransactionView {

    @JsonIgnore
    private Long id;
    private String gatewayAccountId;
    private Long amount;
    private Long totalAmount;
    private Long corporateCardSurcharge;
    private Long fee;
    private Long netAmount;
    private ExternalTransactionState state;
    private String description;
    private String reference;
    private String language;
    private String externalId;
    private String parentExternalId;
    private String returnUrl;
    private String email;
    private String paymentProvider;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private ZonedDateTime createdDate;
    private CardDetails cardDetails;
    private Boolean delayedCapture;
    private String gatewayTransactionId;
    private RefundSummary refundSummary;
    private SettlementSummary settlementSummary;
    private Map<String, Object> metadata;
    private String refundedBy;
    private TransactionType transactionType;

    public TransactionView(Builder builder) {
        this.id = builder.id;
        this.gatewayAccountId = builder.gatewayAccountId;
        this.amount = builder.amount;
        this.totalAmount = builder.totalAmount;
        this.corporateCardSurcharge = builder.corporateCardSurcharge;
        this.fee = builder.fee;
        this.netAmount = builder.netAmount;
        this.state = builder.state;
        this.description = builder.description;
        this.reference = builder.reference;
        this.language = builder.language;
        this.externalId = builder.externalId;
        this.parentExternalId = builder.parentExternalId;
        this.returnUrl = builder.returnUrl;
        this.email = builder.email;
        this.paymentProvider = builder.paymentProvider;
        this.createdDate = builder.createdDate;
        this.cardDetails = builder.cardDetails;
        this.delayedCapture = builder.delayedCapture;
        this.gatewayTransactionId = builder.gatewayTransactionId;
        this.refundSummary = builder.refundSummary;
        this.settlementSummary = builder.settlementSummary;
        this.metadata = builder.metadata;
        this.refundedBy = builder.refundedBy;
        this.transactionType = builder.transactionType;
    }

    public TransactionView() {
    }

    public static TransactionView from(Transaction transaction, int statusVersion) {
        if (transaction instanceof Payment) {
            Payment payment = (Payment) transaction;

            TransactionState state = payment.getState();
            String status = statusVersion == 2 ? state.getStatus() : state.getOldStatus();
            ExternalTransactionState externalState = new ExternalTransactionState(status, state.isFinished(),
                    state.getCode(), state.getMessage());

            return new Builder()
                    .withId(payment.getId())
                    .withGatewayAccountId(payment.getGatewayAccountId())
                    .withAmount(payment.getAmount())
                    .withTotalAmount(payment.getTotalAmount())
                    .withCorporateCardSurcharge(payment.getCorporateCardSurcharge())
                    .withFee(payment.getFee())
                    .withNetAmount(payment.getNetAmount())
                    .withState(externalState)
                    .withDescription(payment.getDescription())
                    .withReference(payment.getReference())
                    .withLanguage(payment.getLanguage())
                    .withExternalId(payment.getExternalId())
                    .withReturnUrl(payment.getReturnUrl())
                    .withEmail(payment.getEmail())
                    .withPaymentProvider(payment.getPaymentProvider())
                    .withCreatedDate(payment.getCreatedDate())
                    .withCardDetails(payment.getCardDetails())
                    .withDelayedCapture(payment.getDelayedCapture())
                    .withGatewayTransactionId(payment.getGatewayTransactionId())
                    .withRefundSummary(payment.getRefundSummary())
                    .withSettlementSummary(payment.getSettlementSummary())
                    .withMetadata(payment.getExternalMetadata())
                    .withTransactionType(payment.getTransactionType())
                    .build();
        }

        Refund refund = (Refund) transaction;
        return new Builder()
                .withId(refund.getId())
                .withGatewayAccountId(refund.getGatewayAccountId())
                .withAmount(refund.getAmount())
                .withState(new ExternalTransactionState(refund.getState().getStatus(), refund.getState().isFinished()))
                .withDescription(refund.getDescription())
                .withReference(refund.getReference())
                .withExternalId(refund.getExternalId())
                .withParentExternalId(refund.getParentExternalId())
                .withCreatedDate(refund.getCreatedDate())
                .withRefundedBy(refund.getRefundedBy())
                .withTransactionType(refund.getTransactionType())
                .build();
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

    public ExternalTransactionState getState() {
        return state;
    }

    public String getDescription() {
        return description;
    }

    public String getReference() {
        return reference;
    }

    public String getLanguage() {
        return language;
    }

    @JsonProperty("transaction_id")
    public String getTransactionId() {
        return externalId;
    }

    @JsonProperty("parent_transaction_id")
    public String getParentTransactionId() {
        return parentExternalId;
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

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionView that = (TransactionView) o;
        return Objects.equals(externalId, that.externalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalId);
    }

    public RefundSummary getRefundSummary() {
        return refundSummary;
    }

    public SettlementSummary getSettlementSummary() {
        return settlementSummary;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Long getCorporateCardSurcharge() {
        return corporateCardSurcharge;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public Long getFee() {
        return fee;
    }

    public Long getNetAmount() {
        return netAmount;
    }

    public String getRefundedBy() {
        return refundedBy;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public static class Builder {
        private Long id;
        private String gatewayAccountId;
        private Long amount;
        private Long totalAmount;
        private Long corporateCardSurcharge;
        private Long fee;
        private Long netAmount;
        private ExternalTransactionState state;
        private String description;
        private String reference;
        private String language;
        private String externalId;
        private String parentExternalId;
        private String returnUrl;
        private String email;
        private String paymentProvider;
        private ZonedDateTime createdDate;
        private CardDetails cardDetails;
        private Boolean delayedCapture;
        private String gatewayTransactionId;
        private RefundSummary refundSummary;
        private SettlementSummary settlementSummary;
        private Map<String, Object> metadata;
        private String refundedBy;
        private TransactionType transactionType;
        private List<Link> links = new ArrayList<>();

        public Builder() {
        }

        public TransactionView build() {
            return new TransactionView(this);
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

        public Builder withState(ExternalTransactionState state) {
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

        public Builder withParentExternalId(String parentExternalId) {
            this.parentExternalId = parentExternalId;
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

        public Builder withSettlementSummary(SettlementSummary settlementSummary) {
            this.settlementSummary = settlementSummary;
            return this;
        }

        public Builder withMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder withRefundedBy(String refundedBy) {
            this.refundedBy = refundedBy;
            return this;
        }

        public Builder withTransactionType(TransactionType transactionType) {
            this.transactionType = transactionType;
            return this;
        }
    }
}
