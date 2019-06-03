package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CardDetails {

    private String cardHolderName;
    private Address billingAddress;
    private String cardBrand;

    public CardDetails(String cardHolderName, Address billingAddress, String cardBrand) {
        this.cardHolderName = cardHolderName;
        this.billingAddress = billingAddress;
        this.cardBrand = cardBrand;
    }

    @JsonProperty("cardholder_name")
    public String getCardHolderName() {
        return cardHolderName;
    }

    @JsonProperty("billing_address")
    public Address getBillingAddress() {
        return billingAddress;
    }

    @JsonProperty("card_brand")
    public String getCardBrand() {
        return cardBrand;
    }
}
