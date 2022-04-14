package uk.gov.pay.ledger.transaction.search.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.ledger.transaction.model.AuthorisationSummary;
import uk.gov.pay.ledger.transaction.model.CardDetails;
import uk.gov.pay.ledger.transaction.model.Payment;
import uk.gov.pay.ledger.transaction.model.Refund;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.ExternalTransactionState;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeSerializer;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.Source;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TransactionView {

    @JsonIgnore
    private Long id;
    @Schema(example = "1")
    private String gatewayAccountId;
    @Schema(example = "baea0585e30d4acbb438524fca82b51e")
    private String serviceId;
    @Schema(example = "7a7a718cb8b0401f8b8f4cdda9804073")
    private String credentialExternalId;
    @Schema(example = "1000")
    private Long amount;
    @Schema(example = "1000")
    private Long totalAmount;
    @Schema(example = "0")
    private Long corporateCardSurcharge;
    @Schema(example = "2")
    private Long fee;
    @Schema(example = "-2")
    private Long netAmount;
    private ExternalTransactionState state;
    @Schema(example = "New passport application")
    private String description;
    @Schema(example = "payment reference")
    private String reference;
    @Schema(example = "en")
    private String language;
    private String externalId;
    private String parentExternalId;
    @Schema(example = "https://service-name.gov.uk/transactions/12345")
    private String returnUrl;
    @Schema(example = "citizen@example.org")
    private String email;
    @Schema(example = "sandbox")
    private String paymentProvider;
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    @Schema(example = "\"2016-01-21T17:15:00Z\"")
    private ZonedDateTime createdDate;
    private CardDetails cardDetails;
    private Boolean delayedCapture;
    @Schema(example = "fd21-1mdknkls1-2121-csdf")
    private String gatewayTransactionId;
    private RefundSummary refundSummary;
    private SettlementSummary settlementSummary;
    private AuthorisationSummary authorisationSummary;
    private Map<String, Object> metadata;
    @Schema(description = "External ID of the service user that refunded payment. Only available for refunds", example = "dj8923eihsdih23hkjfe8d")
    private String refundedBy;
    @Schema(description = "Service user email that refunded the payment. Only available for refunds", example = "test@example.org")
    private String refundedByUserEmail;
    @Schema(example = "PAYMENT")
    private TransactionType transactionType;
    private Boolean moto;
    private Boolean live;
    @Schema(example = "CARD_API")
    private Source source;
    @Schema(example = "APPLE_PAY")
    private String walletType;
    @Schema(example = "po_mdoiu23jkdj1kj23sd")
    private String gatewayPayoutId;
    @Schema(example = "web")
    private AuthorisationMode authorisationMode;
    private TransactionView paymentDetails;

    public TransactionView(Builder builder) {
        this.id = builder.id;
        this.gatewayAccountId = builder.gatewayAccountId;
        this.serviceId = builder.serviceId;
        this.credentialExternalId = builder.credentialExternalId;
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
        this.authorisationSummary = builder.authorisationSummary;
        this.metadata = builder.metadata;
        this.refundedBy = builder.refundedBy;
        this.refundedByUserEmail = builder.refundedByUserEmail;
        this.transactionType = builder.transactionType;
        this.moto = builder.moto;
        this.live = builder.live;
        this.source = builder.source;
        this.walletType = builder.walletType;
        this.gatewayPayoutId = builder.gatewayPayoutId;
        this.authorisationMode = builder.authorisationMode;
        this.paymentDetails = builder.paymentDetails;
    }

    public TransactionView() {
    }

    public static TransactionView from(Transaction transaction, int statusVersion) {
        if (transaction instanceof Payment) {
            Payment payment = (Payment) transaction;

            Builder paymentBuilder = new Builder()
                    .withId(payment.getId())
                    .withGatewayAccountId(payment.getGatewayAccountId())
                    .withServiceId(payment.getServiceId())
                    .withCredentialExternalId(payment.getCredentialExternalId())
                    .withAmount(payment.getAmount())
                    .withTotalAmount(payment.getTotalAmount())
                    .withCorporateCardSurcharge(payment.getCorporateCardSurcharge())
                    .withFee(payment.getFee())
                    .withNetAmount(payment.getNetAmount())
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
                    .withAuthorisationSummary(payment.getAuthorisationSummary())
                    .withMetadata(payment.getExternalMetadata())
                    .withTransactionType(payment.getTransactionType())
                    .withGatewayTransactionId(payment.getGatewayTransactionId())
                    .withMoto(payment.getMoto())
                    .withLive(payment.isLive())
                    .withSource(payment.getSource())
                    .withWalletType(payment.getWalletType())
                    .withGatewayPayoutId(payment.getGatewayPayoutId())
                    .withAuthorisationMode(payment.getAuthorisationMode());
            if (payment.getState() != null) {
                paymentBuilder = paymentBuilder
                        .withState(ExternalTransactionState.from(payment.getState(), statusVersion));
            }
            return paymentBuilder.build();
        }

        Refund refund = (Refund) transaction;
        Builder refundBuilder = new Builder()
                .withId(refund.getId())
                .withServiceId(refund.getServiceId())
                .withLive(refund.getLive())
                .withGatewayAccountId(refund.getGatewayAccountId())
                .withAmount(refund.getAmount())
                .withState(ExternalTransactionState.from(refund.getState(), statusVersion))
                .withGatewayTransactionId(refund.getGatewayTransactionId())
                .withExternalId(refund.getExternalId())
                .withParentExternalId(refund.getParentExternalId())
                .withCreatedDate(refund.getCreatedDate())
                .withRefundedBy(refund.getRefundedBy())
                .withRefundedByUserEmail(refund.getRefundedByUserEmail())
                .withTransactionType(refund.getTransactionType())
                .withGatewayPayoutId(refund.getGatewayPayoutId())
                .withSettlementSummary(refund.getSettlementSummary());
        if (refund.getPaymentDetails() != null) {
            refundBuilder = refundBuilder
                    .withPaymentDetails(from(refund.getPaymentDetails(), statusVersion));
        }
        return refundBuilder.build();
    }

    public Long getId() {
        return id;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getCredentialExternalId() {
        return credentialExternalId;
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
    @Schema(example = "9np5pocnotgkpp029d5kdfau5f")
    public String getTransactionId() {
        return externalId;
    }

    @JsonProperty("parent_transaction_id")
    @Schema(example = "4dfdhapsdgftgkpp029d5kdfau5f", description = "Parent transaction external ID. Available for refunds")
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

    public Boolean getMoto() {
        return moto;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public String getRefundedByUserEmail() {
        return refundedByUserEmail;
    }

    public AuthorisationMode getAuthorisationMode() {
        return authorisationMode;
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

    public AuthorisationSummary getAuthorisationSummary() {
        return authorisationSummary;
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

    public Boolean getLive() {
        return live;
    }

    public Source getSource() {
        return source;
    }

    public String getWalletType() {
        return walletType;
    }

    public String getGatewayPayoutId() {
        return gatewayPayoutId;
    }

    public TransactionView getPaymentDetails() {
        return paymentDetails;
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
        private AuthorisationSummary authorisationSummary;
        private Map<String, Object> metadata;
        private String refundedBy;
        private String refundedByUserEmail;
        private TransactionType transactionType;
        private List<Link> links = new ArrayList<>();
        private Boolean moto;
        private Boolean live;
        private Source source;
        private String walletType;
        private String gatewayPayoutId;
        private TransactionView paymentDetails;
        private String credentialExternalId;
        private String serviceId;
        private AuthorisationMode authorisationMode;

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

        public Builder withAuthorisationSummary(AuthorisationSummary authorisationSummary) {
            this.authorisationSummary = authorisationSummary;
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

        public Builder withRefundedByUserEmail(String refundedByUserEmail) {
            this.refundedByUserEmail = refundedByUserEmail;
            return this;
        }

        public Builder withTransactionType(TransactionType transactionType) {
            this.transactionType = transactionType;
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

        public Builder withPaymentDetails(TransactionView sharedPaymentDetails) {
            this.paymentDetails = sharedPaymentDetails;
            return this;
        }

        public Builder withCredentialExternalId(String credentialExternalId) {
            this.credentialExternalId = credentialExternalId;
            return this;
        }

        public Builder withServiceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder withAuthorisationMode(AuthorisationMode authorisationMode) {
            this.authorisationMode = authorisationMode;
            return this;
        }
    }
}
