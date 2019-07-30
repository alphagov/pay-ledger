package uk.gov.pay.ledger.transaction.service;

import uk.gov.pay.ledger.exception.ValidationException;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;

import java.util.Optional;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class AccountIdSupplierManager {

    private static final String GATEWAY_ACCOUNT_ID = "account_id";

    private final boolean overrideAccountRestriction;
    private final String gatewayAccountId;

    private Supplier<Optional<TransactionView>> privilegedSupplier;
    private Supplier<Optional<TransactionView>> supplier;

    public AccountIdSupplierManager(Boolean overrideAccountRestriction, String gatewayAccountId) {
        this.overrideAccountRestriction = Optional.ofNullable(overrideAccountRestriction).orElse(false);
        this.gatewayAccountId = gatewayAccountId;
    }

    public static AccountIdSupplierManager of(Boolean flag, String gatewayAccountId) {
        return new AccountIdSupplierManager(flag, gatewayAccountId);
    }

    public AccountIdSupplierManager withPrivilegedSupplier(Supplier<Optional<TransactionView>> supplier) {
        this.privilegedSupplier = supplier;
        return this;
    }

    public AccountIdSupplierManager withSupplier(Supplier<Optional<TransactionView>> supplier) {
        this.supplier = supplier;
        return this;
    }

    public Optional<TransactionView> validateAndGet() {
        if(!overrideAccountRestriction && isBlank(gatewayAccountId)) {
            throw new ValidationException(format("Field [%s] cannot be empty", GATEWAY_ACCOUNT_ID));
        }

        if(isBlank(gatewayAccountId)) {
            return privilegedSupplier.get();
        }

        return supplier.get();
    }
}
