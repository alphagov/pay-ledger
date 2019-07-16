package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public class CardDetails {

    private String cardHolderName;
    private Address billingAddress;
    private String cardBrand;
    private String lastDigitsCardNumber;
    private String firstDigitsCardNumber;
    private String expiryDate;

    public CardDetails(String cardHolderName, Address billingAddress, String cardBrand,
                       String lastDigitsCardNumber, String firstDigitsCardNumber, String cardExpiryDate) {
        this.cardHolderName = cardHolderName;
        this.billingAddress = billingAddress;
        this.cardBrand = cardBrand;
        this.lastDigitsCardNumber = lastDigitsCardNumber;
        this.firstDigitsCardNumber = firstDigitsCardNumber;
        this.expiryDate = cardExpiryDate;
    }

    public static CardDetails from(String cardholderName, Address billingAddress, String cardBrand,
                                   String lastDigitsCardNumber, String firstDigitsCardNumber, String cardExpiryDate) {
        if(cardholderName == null &&
                billingAddress == null &&
                cardBrand == null &&
                lastDigitsCardNumber == null &&
                firstDigitsCardNumber == null &&
                cardExpiryDate == null) {
            return null;
        }

        return new CardDetails(cardholderName, billingAddress, cardBrand, lastDigitsCardNumber, firstDigitsCardNumber, cardExpiryDate);
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
        return cardBrand == null ? "" : cardBrand;
    }

    @JsonProperty("last_digits_card_number")
    public String getLastDigitsCardNumber() {
        return lastDigitsCardNumber;
    }

    @JsonProperty("first_digits_card_number")
    public String getFirstDigitsCardNumber() {
        return firstDigitsCardNumber;
    }

    @JsonProperty("expiry_date")
    public String getExpiryDate() {
        return expiryDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardDetails that = (CardDetails) o;
        return Objects.equals(cardHolderName, that.cardHolderName) &&
                Objects.equals(billingAddress, that.billingAddress) &&
                Objects.equals(cardBrand, that.cardBrand) &&
                Objects.equals(lastDigitsCardNumber, that.lastDigitsCardNumber) &&
                Objects.equals(firstDigitsCardNumber, that.firstDigitsCardNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardHolderName, billingAddress, cardBrand, lastDigitsCardNumber, firstDigitsCardNumber);
    }
}
