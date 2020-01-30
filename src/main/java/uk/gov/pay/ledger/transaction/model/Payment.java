package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import uk.gov.pay.commons.api.json.ApiResponseDateTimeSerializer;
import uk.gov.pay.ledger.transaction.search.model.RefundSummary;
import uk.gov.pay.ledger.transaction.search.model.SettlementSummary;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;
import java.util.Map;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Payment extends Transaction{

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
    private SettlementSummary settlementSummary;

    public Payment() {

    }

    public Payment(Long id, String gatewayAccountId, Long amount,
                   String reference, String description, TransactionState state,
                   String language, String externalId, String returnUrl,
                   String email, String paymentProvider, ZonedDateTime createdDate,
                   CardDetails cardDetails, Boolean delayedCapture, Map<String, Object> externalMetaData,
                   Integer eventCount, String gatewayTransactionId, Long corporateCardSurcharge, Long fee,
                   Long netAmount, Long totalAmount, RefundSummary refundSummary, SettlementSummary settlementSummary,
                   Boolean moto) {
        super(id, gatewayAccountId, amount, externalId);
        this.corporateCardSurcharge = corporateCardSurcharge;
        this.fee = fee;
        this.netAmount = netAmount;
        this.totalAmount = totalAmount;
        this.refundSummary = refundSummary;
        this.settlementSummary = settlementSummary;
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
        this.moto = moto;
    }

    public Payment(String gatewayAccountId, Long amount,
                   String reference, String description, TransactionState state,
                   String language, String externalId, String returnUrl,
                   String email, String paymentProvider, ZonedDateTime createdDate,
                   CardDetails cardDetails, Boolean delayedCapture, Map<String, Object> externalMetaData,
                   Integer eventCount, String gatewayTransactionId, Long corporateCardSurcharge, Long fee,
                   Long netAmount, RefundSummary refundSummary, Long totalAmount, SettlementSummary settlementSummary,
                   Boolean moto) {

        this(null, gatewayAccountId, amount, reference, description, state, language, externalId, returnUrl, email,
                paymentProvider, createdDate, cardDetails, delayedCapture, externalMetaData, eventCount,
                gatewayTransactionId, corporateCardSurcharge, fee, netAmount, totalAmount, refundSummary, settlementSummary, moto);
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

    public SettlementSummary getSettlementSummary() {
        return settlementSummary;
    }
}