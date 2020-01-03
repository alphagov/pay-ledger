package uk.gov.pay.ledger.transaction.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.List.of;
import static uk.gov.pay.ledger.transaction.state.TransactionState.CAPTURABLE;
import static uk.gov.pay.ledger.transaction.state.TransactionState.CREATED;
import static uk.gov.pay.ledger.transaction.state.TransactionState.ERROR_GATEWAY;
import static uk.gov.pay.ledger.transaction.state.TransactionState.FAILED_CANCELLED;
import static uk.gov.pay.ledger.transaction.state.TransactionState.FAILED_EXPIRED;
import static uk.gov.pay.ledger.transaction.state.TransactionState.FAILED_REJECTED;
import static uk.gov.pay.ledger.transaction.state.TransactionState.STARTED;
import static uk.gov.pay.ledger.transaction.state.TransactionState.SUBMITTED;

public enum PaymentState {

    IN_PROGRESS(of(CREATED, STARTED, SUBMITTED, CAPTURABLE), "In progress"),
    SUCCESS(of(TransactionState.SUCCESS), "Success"),
    DECLINED(of(FAILED_REJECTED), "Declined"),
    TIMED_OUT(of(FAILED_EXPIRED), "Timed out"),
    CANCELLED(of(FAILED_CANCELLED, TransactionState.CANCELLED), "Cancelled"),
    ERROR(of(TransactionState.ERROR, ERROR_GATEWAY), "Error");

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentState.class);
    private final String displayName;
    private List<TransactionState> states;

    PaymentState(List<TransactionState> states, String displayName) {
        this.states = states;
        this.displayName = displayName;
    }

    public static PaymentState from(TransactionState transactionState) {
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
                .map(paymentState -> paymentState.displayName)
                .orElse(null);
    }

    public List<TransactionState> getStates() {
        return states;
    }
}
