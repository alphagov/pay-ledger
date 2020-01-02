package uk.gov.pay.ledger.transaction.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.List.of;
import static uk.gov.pay.ledger.transaction.state.TransactionState.CREATED;
import static uk.gov.pay.ledger.transaction.state.TransactionState.ERROR_GATEWAY;

public enum RefundState {

    SUBMITTED(of(TransactionState.SUBMITTED, CREATED), "Refund submitted"),
    SUCCESS(of(TransactionState.SUCCESS), "Refund success"),
    ERROR(of(TransactionState.ERROR, ERROR_GATEWAY), "Refund error");

    private static final Logger LOGGER = LoggerFactory.getLogger(RefundState.class);
    private final String displayName;
    private List<TransactionState> states;

    RefundState(List<TransactionState> states, String displayName) {
        this.states = states;
        this.displayName = displayName;
    }

    public static RefundState from(TransactionState transactionState) {
        return stream(values())
                .filter(states -> states.getStates().contains(transactionState))
                .findFirst()
                .orElseGet(() -> {
                    LOGGER.warn("Unknown transaction state {}", transactionState);
                    return null;
                });
    }

    public static String getDisplayName(TransactionState transactionState) {
        return Optional.ofNullable(from(transactionState))
                .map(refundState -> refundState.displayName)
                .orElse(null);
    }

    public List<TransactionState> getStates() {
        return states;
    }
}
