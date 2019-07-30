package uk.gov.pay.ledger.util.fixture;

import com.google.gson.JsonObject;
import io.dropwizard.jackson.Jackson;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.Address;
import uk.gov.pay.ledger.transaction.model.CardDetails;
import uk.gov.pay.ledger.transaction.model.Payment;
import uk.gov.pay.ledger.transaction.model.PaymentFactory;
import uk.gov.pay.ledger.transaction.search.model.RefundSummary;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransactionFixture implements DbFixture<TransactionFixture, TransactionEntity> {

    private Long id = RandomUtils.nextLong(1, 99999);
    private String gatewayAccountId = RandomStringUtils.randomAlphanumeric(10);
    private String externalId = RandomStringUtils.randomAlphanumeric(20);
    private Long amount = RandomUtils.nextLong(1, 99999);
    private String reference = RandomStringUtils.randomAlphanumeric(10);
    private String description = RandomStringUtils.randomAlphanumeric(20);
    private TransactionState state = TransactionState.SUBMITTED;
    private String email = "someone@example.org";
    private String cardholderName = "j.doe@example.org";
    private String externalMetadata = null;
    private ZonedDateTime createdDate = ZonedDateTime.now(ZoneOffset.UTC);
    private String transactionDetails = "{}";
    private Integer eventCount = 1;
    private String cardBrand = "visa";
    private String language = "en";
    private String lastDigitsCardNumber;
    private String firstDigitsCardNumber;
    private CardDetails cardDetails;
    private Boolean delayedCapture = false;
    private String returnUrl = "https://example.org/transactions";
    private String paymentProvider = "sandbox";
    private String gatewayTransactionId;
    private Long corporateCardSurcharge;
    private Long fee;
    private Long netAmount;
    private Long totalAmount;
    private ZonedDateTime settlementSubmittedTime;
    private ZonedDateTime settledTime;
    private String refundStatus = "available";
    private Long refundAmountSubmitted = 0L;
    private Long refundAmountAvailable = 100L;
    private String transactionType;
    private String parentExternalId;


    private TransactionFixture() {
    }

    public static TransactionFixture aTransactionFixture() {
        return new TransactionFixture();
    }

    public static List<TransactionEntity> aTransactionList(String gatewayAccountId, int noOfViews) {
        List<TransactionEntity> transactionList = new ArrayList<>();
        for (int i = 0; i < noOfViews; i++) {
            transactionList.add(aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withDefaultTransactionDetails()
                    .toEntity());
        }
        return transactionList;
    }

    public static List<Payment> aPersistedTransactionList(String gatewayAccountId, int noOfViews, Jdbi jdbi, boolean includeCardDeatils) {
        List<Payment> transactionList = new ArrayList<>();
        long preId = RandomUtils.nextLong();
        for (int i = 0; i < noOfViews; i++) {
            TransactionEntity entity = aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withId(preId + i)
                    .withAmount(100L + i)
                    .withEmail("j.smith@example.org")
                    .withCardholderName("J Smith")
                    .withReference("reference " + i)
                    .withDefaultCardDetails(includeCardDeatils)
                    .withFirstDigitsCardNumber("123456")
                    .withLastDigitsCardNumber("1234")
                    .withDefaultTransactionDetails()
                    .withCardBrand(i % 2 == 0 ? "visa" : "mastercard")
                    .withCreatedDate(ZonedDateTime.now(ZoneOffset.UTC).minusHours(1L).plusMinutes(i))
                    .insert(jdbi)
                    .toEntity();
            transactionList.add(new PaymentFactory(Jackson.newObjectMapper()).createTransactionEntity(entity));
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

    public TransactionFixture withGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
        return this;
    }

    public TransactionFixture withExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public TransactionFixture withParentExternalId(String parentExternalId) {
        this.parentExternalId = parentExternalId;
        return this;
    }

    public TransactionFixture withAmount(Long amount) {
        this.amount = amount;
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

    public TransactionFixture withState(TransactionState state) {
        this.state = state;
        return this;
    }

    public TransactionFixture withEmail(String email) {
        this.email = email;
        return this;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public TransactionFixture withCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
        return this;
    }

    public TransactionFixture withExternalMetadata(String externalMetadata) {
        this.externalMetadata = externalMetadata;
        return this;
    }

    public TransactionFixture withCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public TransactionFixture withDefaultTransactionDetails() {
        transactionDetails = getTransactionDetail().toString();
        return this;
    }

    public String getTransactionDetails() {
        return transactionDetails;
    }

    public TransactionFixture withTransactionDetails(String transactionDetails) {
        this.transactionDetails = transactionDetails;
        return this;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public TransactionFixture withEventCount(Integer eventCount) {
        this.eventCount = eventCount;
        return this;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public TransactionFixture withCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
        return this;
    }

    public TransactionFixture withLanguage(String language) {
        this.language = language;
        return this;
    }

    public String getLastDigitsCardNumber() {
        return lastDigitsCardNumber;
    }

    public TransactionFixture withLastDigitsCardNumber(String lastDigitsCardNumber) {
        this.lastDigitsCardNumber = lastDigitsCardNumber;
        return this;
    }

    public String getFirstDigitsCardNumber() {
        return firstDigitsCardNumber;
    }

    public TransactionFixture withFirstDigitsCardNumber(String firstDigitsCardNumber) {
        this.firstDigitsCardNumber = firstDigitsCardNumber;
        return this;
    }

    public TransactionFixture withCardDetails(CardDetails cardDetails) {
        this.cardDetails = cardDetails;
        return this;
    }

    public TransactionFixture withDelayedCapture(Boolean delayedCapture) {
        this.delayedCapture = delayedCapture;
        return this;
    }

    public TransactionFixture withReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
        return this;
    }

    public TransactionFixture withPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
        return this;
    }

    public TransactionFixture withTransactionType(String transactionType) {
        this.transactionType = transactionType;
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
                                "        external_id,\n" +
                                "        parent_external_id,\n" +
                                "        gateway_account_id,\n" +
                                "        amount,\n" +
                                "        description,\n" +
                                "        reference,\n" +
                                "        state,\n" +
                                "        email,\n" +
                                "        cardholder_name,\n" +
                                "        external_metadata,\n" +
                                "        created_date,\n" +
                                "        transaction_details,\n" +
                                "        event_count,\n" +
                                "        card_brand,\n" +
                                "        last_digits_card_number,\n" +
                                "        first_digits_card_number,\n" +
                                "        total_amount,\n" +
                                "        net_amount,\n" +
                                "        fee,\n" +
                                "        refund_status,\n" +
                                "        refund_amount_submitted,\n" +
                                "        refund_amount_available,\n" +
                                "        settlement_submitted_time,\n" +
                                "        settled_time,\n" +
                                "        type\n" +
                                "    )\n" +
                                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? as jsonb), ?, CAST(? as jsonb), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::transaction_type)\n",
                        id,
                        externalId,
                        parentExternalId,
                        gatewayAccountId,
                        amount,
                        description,
                        reference,
                        state.getState(),
                        email,
                        cardholderName,
                        externalMetadata,
                        createdDate,
                        transactionDetail.toString(),
                        eventCount,
                        cardBrand,
                        lastDigitsCardNumber,
                        firstDigitsCardNumber,
                        totalAmount,
                        netAmount,
                        fee,
                        refundStatus,
                        refundAmountSubmitted,
                        refundAmountAvailable,
                        settlementSubmittedTime,
                        settledTime,
                        transactionType
                )
        );
        return this;
    }

    @NotNull
    private JsonObject getTransactionDetail() {
        JsonObject transactionDetails = new JsonObject();

        transactionDetails.addProperty("language", language);
        transactionDetails.addProperty("return_url", returnUrl);
        transactionDetails.addProperty("payment_provider", paymentProvider);
        transactionDetails.addProperty("delayed_capture", delayedCapture);
        transactionDetails.addProperty("gateway_transaction_id", gatewayTransactionId);
        transactionDetails.addProperty("corporate_surcharge", corporateCardSurcharge);
        Optional.ofNullable(cardDetails)
                .ifPresent(cd -> Optional.ofNullable(cd.getBillingAddress())
                        .ifPresent(ba -> {
                            transactionDetails.addProperty("address_line1", ba.getAddressLine1());
                            transactionDetails.addProperty("address_line2", ba.getAddressLine2());
                            transactionDetails.addProperty("address_postcode", ba.getAddressPostCode());
                            transactionDetails.addProperty("address_city", ba.getAddressCity());
                            transactionDetails.addProperty("address_county", ba.getAddressCounty());
                            transactionDetails.addProperty("address_country", ba.getAddressCountry());
                        }));

        return transactionDetails;
    }

    @Override
    public TransactionEntity toEntity() {
        return new TransactionEntity.Builder()
                .withId(id)
                .withGatewayAccountId(gatewayAccountId)
                .withExternalId(externalId)
                .withAmount(amount)
                .withReference(reference)
                .withDescription(description)
                .withState(state.getState())
                .withEmail(email)
                .withCardholderName(cardholderName)
                .withExternalMetadata(externalMetadata)
                .withCreatedDate(createdDate)
                .withTransactionDetails(transactionDetails)
                .withEventCount(eventCount)
                .withCardBrand(cardBrand)
                .withLastDigitsCardNumber(lastDigitsCardNumber)
                .withFirstDigitsCardNumber(firstDigitsCardNumber)
                .withNetAmount(netAmount)
                .withTotalAmount(totalAmount)
                .withSettlementSubmittedTime(settlementSubmittedTime)
                .withSettledTime(settledTime)
                .withRefundStatus(refundStatus)
                .withRefundAmountSubmitted(refundAmountSubmitted)
                .withRefundAmountAvailable(refundAmountAvailable)
                .withFee(fee)
                .withTransactionType(transactionType)
                .build();
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

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public TransactionState getState() {
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


    public TransactionFixture withDefaultCardDetails(boolean includeCardDetails) {
        if (includeCardDetails) {
            Address billingAddress = new Address("line1", "line2", "AB1 2CD",
                    "London", null, "GB");

            cardDetails = new CardDetails(cardholderName, billingAddress, cardBrand,
                    "1234", "123456", "11/23");
        }
        return this;
    }

    public TransactionFixture withDefaultCardDetails() {
        return this.withDefaultCardDetails(true);
    }


    public TransactionFixture withCorporateCardSurcharge(long value) {
        this.corporateCardSurcharge = value;
        return this;
    }

    public TransactionFixture withFee(long value) {
        this.fee = value;
        return this;
    }

    public TransactionFixture withNetAmount(long value) {
        this.netAmount = value;
        return this;
    }

    public TransactionFixture withRefundSummary(RefundSummary refundSummary) {
        this.refundStatus = refundSummary.getStatus();
        this.refundAmountAvailable = refundSummary.getAmountAvailable();
        this.refundAmountSubmitted = refundSummary.getAmountSubmitted();
        return this;
    }

    public TransactionFixture withTotalAmount(long value) {
        this.totalAmount = value;
        return this;
    }

    public TransactionFixture withSettlementSubmittedTime(ZonedDateTime time) {
        this.settlementSubmittedTime = time;
        return this;
    }

    public TransactionFixture withSettledTime(ZonedDateTime time) {
        this.settledTime = time;
        return this;
    }
}
