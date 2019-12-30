package uk.gov.pay.ledger.util.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.ledger.transaction.model.CardDetails;
import uk.gov.pay.ledger.transaction.model.CardType;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.transaction.state.ExternalTransactionState;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class FlatCsvTransaction {

    @JsonProperty("Reference")
    private String reference;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Email")
    private String email;

    @JsonProperty("Amount")
    private String amount;

    @JsonProperty("Card Brand")
    private String cardBrand;

    @JsonProperty("Cardholder Name")
    private String cardholderName;

    @JsonProperty("Card Expiry Date")
    private String cardExpiryDate;

    @JsonProperty("Card Number")
    private String cardNumber;

    @JsonProperty("State")
    private String state;

    @JsonProperty("Finished")
    private Boolean finished;

    @JsonProperty("Error Code")
    private String errorCode;

    @JsonProperty("Error Message")
    private String errorMessage;

    @JsonProperty("Provider ID")
    private String providerId;

    @JsonProperty("GOV.UK Payment ID")
    private String transactionId;

    @JsonProperty("Issued By")
    private String issuedBy;

    @JsonProperty("Date Created")
    private String dateCreated;

    @JsonProperty("Time Created")
    private String timeCreated;

    @JsonProperty("Corporate Card Surcharge")
    private String corporateSurcharge;

    @JsonProperty("Total Amount")
    private String totalAmount;

    @JsonProperty("Wallet Type")
    private String walletType;

    @JsonProperty("Card Type")
    private String cardType;

    public FlatCsvTransaction() {

    }

    public FlatCsvTransaction(
            String reference, String description, String email, String amount, String cardBrand, String cardholderName,
            String cardExpiryDate, String cardNumber, String state, Boolean finished, String errorCode,
            String errorMessage, String providerId, String transactionId, String issuedBy, String dateCreated,
            String timeCreated, String corporateSurcharge, String totalAmount, String walletType, String cardType
    ) {
        this.reference = reference;
        this.description = description;
        this.email = email;
        this.amount = amount;
        this.cardBrand = cardBrand;
        this.cardholderName = cardholderName;
        this.cardExpiryDate = cardExpiryDate;
        this.cardNumber = cardNumber;
        this.state = state;
        this.finished = finished;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.providerId = providerId;
        this.transactionId = transactionId;
        this.issuedBy = issuedBy;
        this.dateCreated = dateCreated;
        this.timeCreated = timeCreated;
        this.corporateSurcharge = corporateSurcharge;
        this.totalAmount = totalAmount;
        this.walletType = walletType;
        this.cardType = cardType;
    }

    private static String penceToCurrency(Long amount) {
        if (amount != null) {
            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
            return decimalFormat.format(amount / 100);
        }
        return null;
    }

    public static FlatCsvTransaction from(TransactionView transactionView) {
        String amount = penceToCurrency(transactionView.getAmount());
        String totalAmount = penceToCurrency(transactionView.getTotalAmount());
        String corporateSurchargeAmount = penceToCurrency(transactionView.getCorporateCardSurcharge());

        String dateCreated = Optional.ofNullable(transactionView.getCreatedDate())
                .map(date -> date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                .orElse(null);

        String timeCreated = Optional.ofNullable(transactionView.getCreatedDate())
                .map(date -> date.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .orElse(null);

        String cardType = Optional.ofNullable(transactionView.getCardDetails())
                .map(CardDetails::getCardType)
                .map(CardType::toString)
                .orElse(null);

        return new FlatCsvTransaction(
                transactionView.getReference(),
                transactionView.getDescription(),
                transactionView.getEmail(),
                amount,
                Optional.ofNullable(transactionView.getCardDetails()).map(CardDetails::getCardBrand).orElse(null),
                Optional.ofNullable(transactionView.getCardDetails()).map(CardDetails::getCardHolderName).orElse(null),
                Optional.ofNullable(transactionView.getCardDetails()).map(CardDetails::getExpiryDate).orElse(null),
                Optional.ofNullable(transactionView.getCardDetails()).map(CardDetails::getLastDigitsCardNumber).orElse(null),
                Optional.ofNullable(transactionView.getState()).map(ExternalTransactionState::getStatus).orElse(null),
                Optional.ofNullable(transactionView.getState()).map(ExternalTransactionState::isFinished).orElse(null),
                Optional.ofNullable(transactionView.getState()).map(ExternalTransactionState::getCode).orElse(null),
                Optional.ofNullable(transactionView.getState()).map(ExternalTransactionState::getMessage).orElse(null),
                transactionView.getGatewayTransactionId(),
                transactionView.getTransactionId(),
                transactionView.getRefundedBy(),
                dateCreated,
                timeCreated,
                corporateSurchargeAmount,
                totalAmount,
                null,
                cardType
        );
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public String getEmail() {
        return email;
    }

    public String getAmount() {
        return amount;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public String getCardExpiryDate() {
        return cardExpiryDate;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getState() {
        return state;
    }

    public Boolean getFinished() {
        return finished;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public String getCorporateSurcharge() {
        return corporateSurcharge;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public String getCardType() {
        return cardType;
    }

    public String getWalletType() {
        return walletType;
    }
}
