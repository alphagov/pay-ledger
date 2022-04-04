package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.ledger.util.serialiser.ToLowerCaseStringSerializer;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public class CardDetails {

    @Schema(example = "J Doe")
    private String cardHolderName;
    private Address billingAddress;
    @Schema(example = "Visa")
    private String cardBrand;
    @Schema(example = "4242")
    private String lastDigitsCardNumber;
    @Schema(example = "424242")
    private String firstDigitsCardNumber;
    @Schema(example = "11/43")
    private String expiryDate;
    @Schema(example = "credit")
    private CardType cardType;

    public CardDetails(String cardHolderName, Address billingAddress, String cardBrand,
                       String lastDigitsCardNumber, String firstDigitsCardNumber, String cardExpiryDate, CardType cardType) {
        this.cardHolderName = cardHolderName;
        this.billingAddress = billingAddress;
        this.cardBrand = cardBrand;
        this.lastDigitsCardNumber = lastDigitsCardNumber;
        this.firstDigitsCardNumber = firstDigitsCardNumber;
        this.expiryDate = cardExpiryDate;
        this.cardType = cardType;
    }

    public static CardDetails from(String cardholderName, Address billingAddress, String cardBrand,
                                   String lastDigitsCardNumber, String firstDigitsCardNumber, String cardExpiryDate, CardType cardType) {
        if(cardholderName == null &&
                billingAddress == null &&
                cardBrand == null &&
                lastDigitsCardNumber == null &&
                firstDigitsCardNumber == null &&
                cardExpiryDate == null &&
                cardType == null) {
            return null;
        }

        return new CardDetails(cardholderName, billingAddress, cardBrand, lastDigitsCardNumber, firstDigitsCardNumber, cardExpiryDate, cardType);
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

    @Enumerated(EnumType.STRING)
    @JsonProperty("card_type")
    @JsonSerialize(using = ToLowerCaseStringSerializer.class)
    public CardType getCardType() { return cardType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardDetails that = (CardDetails) o;
        return Objects.equals(cardHolderName, that.cardHolderName) &&
                Objects.equals(billingAddress, that.billingAddress) &&
                Objects.equals(cardBrand, that.cardBrand) &&
                Objects.equals(lastDigitsCardNumber, that.lastDigitsCardNumber) &&
                Objects.equals(firstDigitsCardNumber, that.firstDigitsCardNumber) &&
                Objects.equals(cardType, that.cardType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardHolderName, billingAddress, cardBrand, lastDigitsCardNumber, firstDigitsCardNumber, cardType);
    }
}
