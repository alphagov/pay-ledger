package uk.gov.pay.ledger.transaction.dao;

import com.google.gson.JsonObject;
import uk.gov.pay.ledger.transaction.model.CardDetails;
import uk.gov.pay.ledger.transaction.model.Transaction;

import java.time.ZonedDateTime;
import java.util.Optional;

public class TransactionRow {
    private Long id;
    private String gatewayAccountId;
    private Long amount;
    private String reference;
    private String description;
    private String status;
    private String language;
    private String externalId;
    private String returnUrl;
    private String email;
    private String paymentProvider;
    private ZonedDateTime createdAt;
    private CardDetails cardDetails;
    private Boolean delayedCapture;
    private String externalMetaData;

    private TransactionRow(Transaction transaction) {
        this.id = transaction.getId();
        this.gatewayAccountId = transaction.getGatewayAccountId();
        this.amount = transaction.getAmount();
        this.reference = transaction.getReference();
        this.description = transaction.getDescription();
        this.status = transaction.getState();
        this.language = transaction.getLanguage();
        this.externalId = transaction.getExternalId();
        this.returnUrl = transaction.getReturnUrl();
        this.email = transaction.getEmail();
        this.paymentProvider = transaction.getPaymentProvider();
        this.createdAt = transaction.getCreatedAt();
        this.cardDetails = transaction.getCardDetails();
        this.delayedCapture = transaction.getDelayedCapture();
        this.externalMetaData = transaction.getExternalMetaData();
    }

    public Long getId() {
        return id;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public Long getAmount() {
        return amount;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getEmail() {
        return email;
    }

    public ZonedDateTime getCreatedDate() {
        return createdAt;
    }

    public String getExternalMetadata() {
        return externalMetaData;
    }

    public String getCardholderName() {
        return Optional.ofNullable(cardDetails)
                .map(CardDetails::getCardHolderName)
                .orElse(null);
    }

    public String getTransactionDetails() {
        JsonObject transactionDetail = new JsonObject();

        transactionDetail.addProperty("language", language);
        transactionDetail.addProperty("return_url", returnUrl);
        transactionDetail.addProperty("payment_provider", paymentProvider);
        transactionDetail.addProperty("delayed_capture", delayedCapture);
        Optional.ofNullable(cardDetails)
                .ifPresent(cd -> {
                    Optional.ofNullable(cd.getBillingAddress())
                            .ifPresent(ba -> {
                                transactionDetail.addProperty("address_line1", ba.getAddressLine1());
                                transactionDetail.addProperty("address_line2", ba.getAddressLine2());
                                transactionDetail.addProperty("address_postcode", ba.getAddressPostCode());
                                transactionDetail.addProperty("address_city", ba.getAddressCity());
                                transactionDetail.addProperty("address_county", ba.getAddressCounty());
                                transactionDetail.addProperty("address_country", ba.getAddressCountry());
                            });

                });

        return transactionDetail.toString();
    }

    public static TransactionRow fromTransaction(Transaction transaction) {
        return new TransactionRow(transaction);
    }
}
