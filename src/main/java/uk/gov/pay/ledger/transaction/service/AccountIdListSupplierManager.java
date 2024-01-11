package uk.gov.pay.ledger.transaction.service;

import uk.gov.pay.ledger.exception.ValidationException;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;

public class AccountIdListSupplierManager<T> {

    private final boolean overrideAccountRestriction;
    private final Set<String> gatewayAccountIds;

    private Supplier<T> privilegedSupplier;
    private Function<Set<String>, T> supplier;

    public AccountIdListSupplierManager(Boolean overrideAccountRestriction, Set<String> gatewayAccountIds) {
        this.overrideAccountRestriction = Optional.ofNullable(overrideAccountRestriction).orElse(false);
        this.gatewayAccountIds = gatewayAccountIds != null ? gatewayAccountIds : Set.of();
    }

    public static <U> AccountIdListSupplierManager<U> of(Boolean overrideAccountRestriction, Set<String> gatewayAccountIds) {
        return new AccountIdListSupplierManager<>(overrideAccountRestriction, gatewayAccountIds);
    }

    public AccountIdListSupplierManager<T> withPrivilegedSupplier(Supplier<T> supplier) {
        this.privilegedSupplier = supplier;
        return this;
    }

    public AccountIdListSupplierManager<T> withSupplier(Function<Set<String>, T> supplier) {
        this.supplier = supplier;
        return this;
    }

    public T validateAndGet(String fieldName) {
        if (!overrideAccountRestriction && gatewayAccountIds.isEmpty()) {
            throw new ValidationException(format("Field [%s] cannot be empty", fieldName));
        }

        if (gatewayAccountIds.isEmpty()) {
            return privilegedSupplier.get();
        }

        return supplier.apply(gatewayAccountIds);
    }
}
