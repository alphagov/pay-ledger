package uk.gov.pay.ledger.transaction.model;

import static java.util.Arrays.stream;

public enum Exemption3ds {

    EXEMPTION_NOT_REQUESTED("not requested"),
    EXEMPTION_HONOURED("honoured"),
    EXEMPTION_REJECTED("rejected"),
    EXEMPTION_OUT_OF_SCOPE("out of scope");

    private String displayName;

    Exemption3ds(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static Exemption3ds from(String exemption3ds) {
        return stream(values()).filter(v -> v.name().equals(exemption3ds)).findFirst()
                .orElse(null);
    }
}
