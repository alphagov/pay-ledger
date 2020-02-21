package uk.gov.pay.ledger.transaction.service;

import uk.gov.pay.ledger.exception.ValidationException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;

public class AccountIdListSupplierManager<T> {

    private static final String ACCOUNT_ID = "account_id";

    private final boolean overrideAccountRestriction;
    private final List<String> gatewayAccountIds;

    private Supplier<T> privilegedSupplier;
    private Function<List<String>, T> supplier;

    public AccountIdListSupplierManager(Boolean overrideAccountRestriction, List<String> gatewayAccountIds) {
        this.overrideAccountRestriction = Optional.ofNullable(overrideAccountRestriction).orElse(false);
        this.gatewayAccountIds = gatewayAccountIds != null ? gatewayAccountIds : List.of();
    }

    public static <U> AccountIdListSupplierManager<U> of(Boolean overrideAccountRestriction, List<String> gatewayAccountIds) {
        return new AccountIdListSupplierManager<>(overrideAccountRestriction, gatewayAccountIds);
    }

    public AccountIdListSupplierManager<T> withPrivilegedSupplier(Supplier<T> supplier) {
        this.privilegedSupplier = supplier;
        return this;
    }

    public AccountIdListSupplierManager<T> withSupplier(Function<List<String>, T> supplier) {
        this.supplier = supplier;
        return this;
    }

    public T validateAndGet() {
        if (!overrideAccountRestriction && gatewayAccountIds.isEmpty()) {
            throw new ValidationException(format("Field [%s] cannot be empty", ACCOUNT_ID));
        }

        if (gatewayAccountIds.isEmpty()) {
            return privilegedSupplier.get();
        }

        return supplier.apply(gatewayAccountIds);
    }
}
