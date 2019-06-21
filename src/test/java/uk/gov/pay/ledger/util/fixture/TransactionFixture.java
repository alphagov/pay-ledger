package uk.gov.pay.ledger.util.fixture;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;
import uk.gov.pay.ledger.transaction.model.Address;
import uk.gov.pay.ledger.transaction.model.CardDetails;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransactionFixture implements DbFixture<TransactionFixture, Transaction> {

    private Long id = RandomUtils.nextLong(1, 99999);
    private String externalId = RandomStringUtils.randomAlphanumeric(20);
    private Long amount = RandomUtils.nextLong(1, 99999);
    private String reference = RandomStringUtils.randomAlphanumeric(10);
    private String description = RandomStringUtils.randomAlphanumeric(20);
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneOffset.UTC);
    private String state = "CREATED";
    private String gatewayAccountId = RandomStringUtils.randomAlphanumeric(10);
    private String language = "en";
    private String returnUrl = "https://example.org";
    private String email = "someone@example.org";
    private String paymentProvider = "sandbox";
    private CardDetails cardDetails = getDefaultCardDetails();

    private Boolean delayedCapture = false;

    private String externalMetadata = null;

    private TransactionFixture() {
    }

    public static TransactionFixture aTransactionFixture() {
        return new TransactionFixture();
    }

    public static List<Transaction> aTransactionList(String gatewayAccountId, int noOfViews) {
        List<Transaction> transactionList = new ArrayList<>();
        for (int i = 0; i < noOfViews; i++) {
            transactionList.add(aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .toEntity());
        }
        return transactionList;
    }

    public static List<Transaction> aPersistedTransactionList(String gatewayAccountId, int noOfViews, Jdbi jdbi) {
        List<Transaction> transactionList = new ArrayList<>();
        long preId = RandomUtils.nextLong();
        for (int i = 0; i < noOfViews; i++) {
            transactionList.add(aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withId(preId + i)
                    .withAmount(100L + i)
                    .withReference("reference " + i)
                    .insert(jdbi)
                    .toEntity());
        }
        return transactionList;
    }

    public Long getId() {
        return id;
    }

    public TransactionFixture withId(Long id) {
        this.id = id;
        return this;
    }

    public TransactionFixture withGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
        return this;
    }

    public TransactionFixture withExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public TransactionFixture withAmount(Long amount) {
        this.amount = amount;
        return this;
    }

    public TransactionFixture withState(String state) {
        this.state = state;
        return this;
    }

    public TransactionFixture withReference(String reference) {
        this.reference = reference;
        return this;
    }

    public TransactionFixture withDescription(String description) {
        this.description = description;
        return this;
    }

    public TransactionFixture withCreatedDate(ZonedDateTime createdDate) {
        this.createdAt = createdDate;
        return this;
    }

    public TransactionFixture withLanguage(String language) {
        this.language = language;
        return this;
    }

    public TransactionFixture withReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
        return this;
    }

    public TransactionFixture withEmail(String email) {
        this.email = email;
        return this;
    }

    public TransactionFixture withPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
        return this;
    }

    public TransactionFixture withCardDetails(CardDetails cardDetails) {
        this.cardDetails = cardDetails;
        return this;
    }

    public TransactionFixture withDelayedCapture(boolean delayedCapture) {
        this.delayedCapture = delayedCapture;
        return this;
    }

    public TransactionFixture withExternalMetadata(String externalMetadata) {
        this.externalMetadata = externalMetadata;
        return this;
    }

    @Override
    public TransactionFixture insert(Jdbi jdbi) {
        JsonObject transactionDetail = getTransactionDetail();
        jdbi.withHandle(h ->
                h.execute(
                        "INSERT INTO" +
                                "    transaction(\n" +
                                "        id,\n" +
                                "        gateway_account_id,\n" +
                                "        external_id,\n" +
                                "        amount,\n" +
                                "        reference,\n" +
                                "        description,\n" +
                                "        status,\n" +
                                "        cardholder_name,\n" +
                                "        external_metadata,\n" +
                                "        created_date,\n" +
                                "        email,\n" +
                                "        transaction_details\n" +
                                "    )\n" +
                                "   VALUES(?, ?, ?, ?, ?, ?, ?, ?, CAST(? as jsonb), ?, ?, CAST(? as jsonb))\n",
                        id,
                        gatewayAccountId,
                        externalId,
                        amount,
                        reference,
                        description,
                        state,
                        cardDetails != null ? cardDetails.getCardHolderName() : null,
                        externalMetadata,
                        createdAt,
                        email,
                        transactionDetail.toString()
                )
        );
        return this;
    }

    @NotNull
    private JsonObject getTransactionDetail() {
        JsonObject transactionDetail = new JsonObject();

        transactionDetail.addProperty("language", language);
        transactionDetail.addProperty("return_url", returnUrl);
        transactionDetail.addProperty("payment_provider", paymentProvider);
        transactionDetail.addProperty("delayed_capture", delayedCapture);
        Optional.ofNullable(cardDetails)
                .ifPresent(cd -> Optional.ofNullable(cd.getBillingAddress())
                            .ifPresent(ba -> {
                                transactionDetail.addProperty("address_line1", ba.getAddressLine1());
                                transactionDetail.addProperty("address_line2", ba.getAddressLine2());
                                transactionDetail.addProperty("address_postcode", ba.getAddressPostCode());
                                transactionDetail.addProperty("address_city", ba.getAddressCity());
                                transactionDetail.addProperty("address_county", ba.getAddressCounty());
                                transactionDetail.addProperty("address_country", ba.getAddressCountry());
                            }));

        return transactionDetail;
    }

    @Override
    public Transaction toEntity() {
        return new Transaction(id, gatewayAccountId, amount,
                reference, description, TransactionState.valueOf(state),
                language, externalId, returnUrl,
                email, paymentProvider, createdAt,
                cardDetails, delayedCapture, externalMetadata);
    }

    public String getExternalId() {
        return externalId;
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

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public String getState() {
        return state;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public String getLanguage() {
        return language;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public CardDetails getCardDetails() {
        return cardDetails;
    }

    public Boolean getDelayedCapture() {
        return delayedCapture;
    }

    public String getExternalMetadata() {
        return externalMetadata;
    }

    private CardDetails getDefaultCardDetails() {
        Address billingAddress = new Address("line1", "line2", "AB1 2CD",
                "London", null, "GB");

        return new CardDetails("J. Smith", billingAddress, null);
    }

}
