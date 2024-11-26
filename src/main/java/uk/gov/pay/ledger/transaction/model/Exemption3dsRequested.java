package uk.gov.pay.ledger.transaction.model;

import static java.util.Arrays.stream;

public enum Exemption3dsRequested {

    OPTIMISED("optimised"),
    CORPORATE("corporate");

    private String displayName;

    Exemption3dsRequested(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static Exemption3dsRequested from(String exemption3dsRequested) {
        return stream(values()).filter(v -> v.name().equals(exemption3dsRequested)).findFirst()
                .orElse(null);
    }
}
