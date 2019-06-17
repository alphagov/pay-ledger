package uk.gov.pay.ledger.event.model;

import uk.gov.pay.ledger.transaction.state.TransactionState;

public enum EventType {
    PAYMENT_CREATED(TransactionState.CREATED);

    private final TransactionState transactionState;

    EventType(TransactionState transactionState) {
        this.transactionState = transactionState;
    }

    public TransactionState getTransactionState() {
        return transactionState;
    }
}
