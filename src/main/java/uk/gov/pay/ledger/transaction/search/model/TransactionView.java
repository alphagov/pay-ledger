package uk.gov.pay.ledger.transaction.search.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.commons.api.json.ApiResponseDateTimeSerializer;
import uk.gov.pay.ledger.transaction.model.CardDetails;
import uk.gov.pay.ledger.transaction.model.Payment;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private TransactionState state;
    private String description;
    private String reference;
    private String language;
    private String externalId;
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
    private List<Link> links = new ArrayList<>();

    //todo: replace with builder
    private TransactionView(Long id, String gatewayAccountId, Long amount, Long totalAmount, Long corporateCardSurcharge, Long fee, Long netAmount, TransactionState state,
                            String description, String reference, String language, String externalId,
                            String returnUrl, String email, String paymentProvider, ZonedDateTime createdDate,
                            CardDetails cardDetails, Boolean delayedCapture, String gatewayTransactionId,
                            RefundSummary refundSummary, SettlementSummary settlementSummary, Map<String, Object> metadata) {
        this.id = id;
        this.gatewayAccountId = gatewayAccountId;
        this.amount = amount;
        this.totalAmount = totalAmount;
        this.corporateCardSurcharge = corporateCardSurcharge;
        this.fee = fee;
        this.netAmount = netAmount;
        this.state = state;
        this.description = description;
        this.reference = reference;
        this.language = language;
        this.externalId = externalId;
        this.returnUrl = returnUrl;
        this.email = email;
        this.paymentProvider = paymentProvider;
        this.createdDate = createdDate;
        this.cardDetails = cardDetails;
        this.delayedCapture = delayedCapture;
        this.gatewayTransactionId = gatewayTransactionId;
        this.refundSummary = refundSummary;
        this.settlementSummary = settlementSummary;
        this.metadata = metadata;
    }

    public TransactionView() {
    }

    public static TransactionView from(Payment transaction) {
        return new TransactionView(transaction.getId(), transaction.getGatewayAccountId(),
                transaction.getAmount(), transaction.getTotalAmount(), transaction.getCorporateCardSurcharge(),
                transaction.getFee(), transaction.getNetAmount(), transaction.getState(),
                transaction.getDescription(), transaction.getReference(), transaction.getLanguage(),
                transaction.getExternalId(), transaction.getReturnUrl(), transaction.getEmail(),
                transaction.getPaymentProvider(), transaction.getCreatedDate(), transaction.getCardDetails(),
                transaction.getDelayedCapture(), transaction.getGatewayTransactionId(), transaction.getRefundSummary(),
                transaction.getSettlementSummary(), transaction.getExternalMetadata());
    }

    public TransactionView addLink(Link link) {
        links.add(link);
        return this;
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

    public TransactionState getState() {
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

    public List<Link> getLinks() {
        return links;
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
}
