package uk.gov.pay.ledger.transaction.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.util.Arrays.stream;

public enum DisputeState {

    NEEDS_RESPONSE(TransactionState.NEEDS_RESPONSE, "Dispute awaiting evidence"),
    UNDER_REVIEW(TransactionState.UNDER_REVIEW, "Dispute under review"),
    LOST(TransactionState.LOST, "Dispute lost to customer"),
    WON(TransactionState.WON, "Dispute won in your favour");

    private static final Logger LOGGER = LoggerFactory.getLogger(DisputeState.class);
    private final String displayName;
    private TransactionState transactionState;

    DisputeState(TransactionState transactionState, String displayName) {
        this.transactionState = transactionState;
        this.displayName = displayName;
    }

    public static DisputeState from(TransactionState transactionState) {
        return stream(values())
                .filter(states -> states.getTransactionState() == transactionState)
                .findFirst()
                .orElseGet(() -> {
                    LOGGER.warn("Unknown transaction state {}", transactionState);
                    return null;
                });
    }

    public static String getDisplayName(TransactionState transactionState) {
        return Optional.ofNullable(from(transactionState))
                .map(disputeState -> disputeState.displayName)
                .orElse(null);
    }

    public TransactionState getTransactionState() {
        return transactionState;
    }
}
