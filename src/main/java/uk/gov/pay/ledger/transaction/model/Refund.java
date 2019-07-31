package uk.gov.pay.ledger.transaction.model;

import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;

public class Refund extends Transaction {
    private final String reference;
    private final String description;
    private final TransactionState state;
    private final ZonedDateTime createdDate;
    private final Integer eventCount;
    private final String refundedBy;

    public Refund(String gatewayAccountId, Long amount, String reference, String description, TransactionState state,
                  String externalId, ZonedDateTime createdDate, Integer eventCount, String refundedBy) {
        super(null, gatewayAccountId, amount, externalId);
        this.reference = reference;
        this.description = description;
        this.state = state;
        this.createdDate = createdDate;
        this.eventCount = eventCount;
        this.refundedBy = refundedBy;
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

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public String getRefundedBy() {
        return refundedBy;
    }
}
