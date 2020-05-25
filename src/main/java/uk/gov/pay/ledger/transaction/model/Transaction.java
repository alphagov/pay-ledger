package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public abstract class Transaction {

    @JsonIgnore
    protected Long id;
    protected String gatewayAccountId;
    protected Long amount;
    protected String externalId;
    private String gatewayPayoutId;

    public Transaction() {
    }

    public Transaction(Long id, String gatewayAccountId, Long amount, String externalId, String gatewayPayoutId) {
        this.id = id;
        this.gatewayAccountId = gatewayAccountId;
        this.amount = amount;
        this.externalId = externalId;
        this.gatewayPayoutId = gatewayPayoutId;
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

    public abstract TransactionType getTransactionType();

    public String getExternalId() {
        return externalId;
    }

    public String getGatewayPayoutId() {
        return gatewayPayoutId;
    }
}
