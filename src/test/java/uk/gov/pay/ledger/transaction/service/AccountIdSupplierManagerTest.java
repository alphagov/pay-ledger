package uk.gov.pay.ledger.transaction.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.exception.ValidationException;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AccountIdSupplierManagerTest {

    @Mock
    private Function<List<String>, Optional<TransactionView>> supplier;

    @Mock
    private Supplier<Optional<TransactionView>> privilegedSupplier;

    @Test
    public void givenAccountIdNotProvidedWhenRequiredThrowsAnError() {
        ValidationException validationException = assertThrows(ValidationException.class,
                () -> setUpAndGet(null, null));

        assertThat(validationException.getMessage(), is("Field [account_id] cannot be empty"));

        verify(supplier, never()).apply(any());
        verify(privilegedSupplier, never()).get();
    }

    @Test
    public void givenAccountIdProvidedWhenRequiredUsesSupplier() {
        setUpAndGet(null, List.of("some-id"));

        verify(supplier).apply(List.of("some-id"));
        verify(privilegedSupplier, never()).get();
    }

    @Test
    public void givenAccountIdProvidedWhenNotRequiredUsesSupplier() {
        setUpAndGet(true, List.of("some-id"));

        verify(supplier).apply(List.of("some-id"));
        verify(privilegedSupplier, never()).get();
    }

    @Test
    public void givenAccountIdNotProvidedWhenNotRequiredUsesPrivilegedSupplier() {
        setUpAndGet(true, null);

        verify(supplier, never()).apply(any());
        verify(privilegedSupplier).get();
    }

    private void setUpAndGet(Boolean overrideAccountRestriction, List<String> gatewayAccountId) {
        new AccountIdListSupplierManager(overrideAccountRestriction, gatewayAccountId)
                .withPrivilegedSupplier(privilegedSupplier)
                .withSupplier(supplier)
                .validateAndGet("account_id");
    }
}