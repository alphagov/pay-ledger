package uk.gov.pay.ledger.transaction.service;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.exception.ValidationException;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AccountIdSupplierManagerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private Function<List<String>, Optional<TransactionView>> supplier;

    @Mock
    private Supplier<Optional<TransactionView>> privilegedSupplier;

    @Test
    public void givenAccountIdNotProvidedWhenRequiredThrowsAnError() {
        thrown.expect(ValidationException.class);
        thrown.expectMessage("Field [account_id] cannot be empty");

        setUpAndGet(null, null);

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
                .validateAndGet();
    }
}