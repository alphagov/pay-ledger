package uk.gov.pay.ledger.transaction.model;

import java.util.Optional;

public enum FeeType {
    TRANSACTION("transaction", "Fee (transaction)"),
    RADAR("radar", "Fee (fraud protection)"),
    THREE_DS("three_ds", "Fee (3DS)"),
    GATEWAY("gateway", "Fee (gateway)"),
    FRAUD_PROTECTION("fraud_protection", "Fee (fraud protection)");

    private final String name;
    private final String csvFieldName;


    FeeType(String name, String csvFieldName) {
        this.name = name;
        this.csvFieldName = csvFieldName;
    }

    static Optional<FeeType> fromName(String feeType) {
        for (FeeType value : FeeType.values()) {
            if (value.name.equals(feeType)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    String getCsvFieldName() {
        return csvFieldName;
    }
}
