package uk.gov.pay.ledger.transaction.service;

import uk.gov.pay.ledger.exception.ValidationException;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class AccountIdSupplierManager<T> {

    private static final String GATEWAY_ACCOUNT_ID = "account_id";

    private final boolean overrideAccountRestriction;
    private final String gatewayAccountId;

    private Supplier<T> privilegedSupplier;
    private Function<String, T> supplier;

    public AccountIdSupplierManager(Boolean overrideAccountRestriction, String gatewayAccountId) {
        this.overrideAccountRestriction = Optional.ofNullable(overrideAccountRestriction).orElse(false);
        this.gatewayAccountId = gatewayAccountId;
    }

    public static <U> AccountIdSupplierManager<U> of(Boolean overrideAccountRestriction, String gatewayAccountId) {
        return new AccountIdSupplierManager<>(overrideAccountRestriction, gatewayAccountId);
    }

    public AccountIdSupplierManager<T> withPrivilegedSupplier(Supplier<T> supplier) {
        this.privilegedSupplier = supplier;
        return this;
    }

    public AccountIdSupplierManager<T> withSupplier(Function<String, T> supplier) {
        this.supplier = supplier;
        return this;
    }

    public T validateAndGet() {
        if(!overrideAccountRestriction && isBlank(gatewayAccountId)) {
            throw new ValidationException(format("Field [%s] cannot be empty", GATEWAY_ACCOUNT_ID));
        }

        if(isBlank(gatewayAccountId)) {
            return privilegedSupplier.get();
        }

        return supplier.apply(gatewayAccountId);
    }
}
