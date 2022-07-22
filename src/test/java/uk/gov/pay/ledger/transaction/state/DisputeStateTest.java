package uk.gov.pay.ledger.transaction.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.pay.ledger.transaction.state.DisputeState.getDisplayName;
import static uk.gov.pay.ledger.transaction.state.TransactionState.LOST;
import static uk.gov.pay.ledger.transaction.state.TransactionState.NEEDS_RESPONSE;
import static uk.gov.pay.ledger.transaction.state.TransactionState.SUBMITTED;
import static uk.gov.pay.ledger.transaction.state.TransactionState.UNDER_REVIEW;
import static uk.gov.pay.ledger.transaction.state.TransactionState.WON;

class DisputeStateTest {

    @ParameterizedTest
    @MethodSource(value = "getTransactionStateAndExpectedValues")
    void shouldReturnDisputeStateCorrectlyForValidTransactionState(TransactionState transactionState,
                                                                   DisputeState expectedDisputeState,
                                                                   String expectedDisplayName) {
        DisputeState actualDisputeState = DisputeState.from(transactionState);

        assertThat(actualDisputeState, is(expectedDisputeState));
        assertThat(getDisplayName(transactionState), is(expectedDisplayName));
    }

    @Test
    void shouldReturnNullDisplayStateForUnknownTransactionState() {
        DisputeState actualDisputeState = DisputeState.from(SUBMITTED);

        assertThat(actualDisputeState, is(nullValue()));
        assertThat(getDisplayName(SUBMITTED), is(nullValue()));
    }

    @Test
    void shouldReturnNullDisplayStateForNullTransactionState() {
        DisputeState actualDisputeState = DisputeState.from(null);

        assertThat(actualDisputeState, is(nullValue()));
        assertThat(getDisplayName(null), is(nullValue()));
    }

    private static Stream<Arguments> getTransactionStateAndExpectedValues() {
        return Stream.of(
                Arguments.of(WON, DisputeState.WON, "Dispute won in your favour"),
                Arguments.of(LOST, DisputeState.LOST, "Dispute lost to customer"),
                Arguments.of(UNDER_REVIEW, DisputeState.UNDER_REVIEW, "Dispute under review"),
                Arguments.of(NEEDS_RESPONSE, DisputeState.NEEDS_RESPONSE, "Dispute awaiting evidence")
        );
    }
}