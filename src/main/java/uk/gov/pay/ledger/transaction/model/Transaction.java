package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public abstract class Transaction {

    @JsonIgnore
    protected Long id;
    protected String gatewayAccountId;
    protected String serviceId;
    protected Boolean live;
    protected Long amount;
    protected String externalId;
    private String gatewayPayoutId;

    public Transaction() {
    }

    public Transaction(Long id, String gatewayAccountId, String serviceId, Boolean live, Long amount, String externalId, String gatewayPayoutId) {
        this.id = id;
        this.gatewayAccountId = gatewayAccountId;
        this.serviceId = serviceId;
        this.live = live;
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

    public String getServiceId() {
        return serviceId;
    }

    public Boolean getLive() {
        return live;
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
