package uk.gov.pay.ledger.transaction.model;

public enum CardType {
    CREDIT, DEBIT;

    public static CardType fromString(String stringCardType) {
        if(stringCardType == null) {
            return null;
        } else if(stringCardType.equalsIgnoreCase("credit")) {
            return CREDIT;
        } else if (stringCardType.equalsIgnoreCase("debit")) {
            return DEBIT;
        }
        return null;
    }
}
