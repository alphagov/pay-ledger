package uk.gov.pay.ledger.util.fixture;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.dropwizard.jackson.Jackson;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.Address;
import uk.gov.pay.ledger.transaction.model.CardDetails;
import uk.gov.pay.ledger.transaction.model.Exemption3ds;
import uk.gov.pay.ledger.transaction.model.Exemption3dsRequested;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.model.TransactionFactory;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.search.model.RefundSummary;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.Source;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.pay.ledger.transaction.model.CardType.CREDIT;
import static uk.gov.pay.ledger.util.fixture.MetadataKeyFixture.insertMedataKeyIfNotExists;
import static uk.gov.pay.ledger.util.fixture.TransactionMetadataFixture.aTransactionMetadataFixture;

public class TransactionFixture implements DbFixture<TransactionFixture, TransactionEntity> {

    private Long id = RandomUtils.nextLong(1, 99999);
    private String serviceId = RandomStringUtils.randomAlphanumeric(26);
    private String gatewayAccountId = RandomStringUtils.randomAlphanumeric(10);
    private final String credentialExternalId = "credential-external-id";
    private String externalId = RandomStringUtils.randomAlphanumeric(20);
    private Long amount = RandomUtils.nextLong(1, 99999);
    private String reference = RandomStringUtils.randomAlphanumeric(10);
    private String description = RandomStringUtils.randomAlphanumeric(20);
    private TransactionState state = TransactionState.SUBMITTED;
    private String email = "j.doe@example.org";
    private String cardholderName = "J Doe";
    private JsonElement externalMetadata = null;
    private ZonedDateTime createdDate = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    private String transactionDetails = "{}";
    private Integer eventCount = 1;
    private String cardBrand = "visa";
    private final String cardExpiryDate = "10/21";
    private String language = "en";
    private String lastDigitsCardNumber;
    private String firstDigitsCardNumber;
    private Exemption3dsRequested exemption3dsRequested = null;
    private Exemption3ds exemption3ds = null;
    private CardDetails cardDetails;
    private Boolean delayedCapture = false;
    private String returnUrl = "https://example.org/transactions";
    private String paymentProvider = "sandbox";
    private String gatewayTransactionId;
    private Long corporateCardSurcharge;
    private Long fee;
    private Long netAmount;
    private Long totalAmount;
    private ZonedDateTime captureSubmittedDate;
    private ZonedDateTime capturedDate;
    private String refundStatus = "available";
    private Long refundAmountRefunded = 0L;
    private Long refundAmountAvailable = 100L;
    private String transactionType = TransactionType.PAYMENT.name();
    private String parentExternalId;
    private String refundedById;
    private String cardBrandLabel;
    private boolean live;
    private boolean moto;
    private String refundedByUserEmail;
    private String source;
    private String gatewayPayoutId;
    private String version3ds;
    private Boolean requires3ds = true;
    private String agreementId;
    private Boolean disputed;
    private AuthorisationMode authorisationMode = AuthorisationMode.WEB;
    private Boolean canRetry;

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

    public static List<Transaction> aPersistedTransactionList(String gatewayAccountId, int noOfViews, Jdbi jdbi, boolean includeCardDeatils) {
        List<Transaction> transactionList = new ArrayList<>();
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
                    .withTransactionType(i % 2 == 0 ? "REFUND" : "PAYMENT")
                    .withCreatedDate(ZonedDateTime.now(ZoneOffset.UTC).minusHours(1L).plusMinutes(i))
                    .insert(jdbi)
                    .toEntity();
            transactionList.add(new TransactionFactory(Jackson.newObjectMapper()).createTransactionEntity(entity));
        }
        return transactionList;
    }

    public Long getId() {
        return id;
    }

    public String getCardExpiryDate() {
        return cardExpiryDate;
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

    public TransactionFixture withExternalMetadata(Map<String, Object> externalMetadata) {
        this.externalMetadata = new Gson().toJsonTree(externalMetadata);
        return this;
    }

    public TransactionFixture withExternalMetadata(String externalMetadata) {
        this.externalMetadata = JsonParser.parseString(externalMetadata);
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

    public String getCardBrandLabel() {
        return cardBrandLabel;
    }

    public TransactionFixture withCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
        return this;
    }

    public boolean isMoto() {
        return moto;
    }

    public TransactionFixture withMoto(boolean moto) {
        this.moto = moto;
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

    public Long getFee() {
        return fee;
    }

    public Long getNetAmount() {
        return netAmount;
    }

    public TransactionFixture withFirstDigitsCardNumber(String firstDigitsCardNumber) {
        this.firstDigitsCardNumber = firstDigitsCardNumber;
        return this;
    }

    public TransactionFixture withCardDetails(CardDetails cardDetails) {
        this.cardDetails = cardDetails;
        return this;
    }

    public TransactionFixture withExemption3ds(Exemption3ds exemption3ds) {
        this.exemption3ds = exemption3ds;
        return this;
    }

    public TransactionFixture withExemption3dsRequested(Exemption3dsRequested exemption3dsRequested) {
        this.exemption3dsRequested = exemption3dsRequested;
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

    public TransactionFixture withCardBrandLabel(String cardBrandLabel) {
        this.cardBrandLabel = cardBrandLabel;
        return this;
    }

    public TransactionFixture withLive(boolean live) {
        this.live = live;
        return this;
    }

    public TransactionFixture withSource(String source) {
        this.source = source;
        return this;
    }

    public TransactionFixture withGatewayPayoutId(String gatewayPayoutId) {
        this.gatewayPayoutId = gatewayPayoutId;
        return this;
    }

    public TransactionFixture insertTransactionAndTransactionMetadata(Jdbi jdbi) {
        insert(jdbi);
        insertTransactionMetadata(jdbi, externalMetadata);
        return this;
    }

    public TransactionFixture withVersion3ds(String version3ds) {
        this.version3ds = version3ds;
        return this;
    }

    public TransactionFixture withServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public TransactionFixture withAgreementId(String agreementId) {
        this.agreementId = agreementId;
        return this;
    }

    public TransactionFixture withDisputed(boolean disputed) {
        this.disputed = disputed;
        return this;
    }

    public TransactionFixture withCanRetry(Boolean canRetry) {
        this.canRetry = canRetry;
        return this;
    }

    public TransactionFixture withAuthorisationMode(AuthorisationMode authorisationMode) {
        this.authorisationMode = authorisationMode;
        return this;
    }

    public TransactionFixture withRequires3ds(Boolean requires3ds) {
        this.requires3ds = requires3ds;
        return this;
    }

    @Override
    public TransactionFixture insert(Jdbi jdbi) {
        jdbi.withHandle(h ->
                h.execute(
                        "INSERT INTO" +
                                "    transaction(\n" +
                                "        id,\n" +
                                "        external_id,\n" +
                                "        parent_external_id,\n" +
                                "        service_id,\n" +
                                "        gateway_account_id,\n" +
                                "        amount,\n" +
                                "        description,\n" +
                                "        reference,\n" +
                                "        state,\n" +
                                "        email,\n" +
                                "        cardholder_name,\n" +
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
                                "        refund_amount_refunded,\n" +
                                "        refund_amount_available,\n" +
                                "        type,\n" +
                                "        live,\n" +
                                "        moto,\n" +
                                "        gateway_transaction_id,\n" +
                                "        source,\n" +
                                "        gateway_payout_id,\n" +
                                "        agreement_id\n" +
                                "    )\n" +
                                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? as jsonb), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::transaction_type, ?, ?, ?, ?::source, ?, ?)\n",
                        id,
                        externalId,
                        parentExternalId,
                        serviceId,
                        gatewayAccountId,
                        amount,
                        description,
                        reference,
                        state,
                        email,
                        cardholderName,
                        createdDate,
                        transactionDetails,
                        eventCount,
                        cardBrand,
                        lastDigitsCardNumber,
                        firstDigitsCardNumber,
                        totalAmount,
                        netAmount,
                        fee,
                        refundStatus,
                        refundAmountRefunded,
                        refundAmountAvailable,
                        transactionType,
                        live,
                        moto,
                        gatewayTransactionId,
                        source,
                        gatewayPayoutId,
                        agreementId
                )
        );
        return this;
    }

    private void insertTransactionMetadata(Jdbi jdbi, JsonElement externalMetadata) {
        Optional.ofNullable(externalMetadata)
                .ifPresent(cd ->
                        cd.getAsJsonObject()
                                .entrySet()
                                .forEach(metadataJsonElement -> {
                                    if (!metadataJsonElement.getValue().isJsonObject()) {
                                        insertMedataKeyIfNotExists(jdbi, metadataJsonElement.getKey());
                                        aTransactionMetadataFixture().withTransactionId(id)
                                                .withMetadataKey(metadataJsonElement.getKey())
                                                .withValue(metadataJsonElement.getValue().getAsString())
                                                .insert(jdbi);
                                    }
                                }));
    }

    @NotNull
    private JsonObject getTransactionDetail() {
        JsonObject transactionDetails = new JsonObject();
        JsonObject refundPaymentDetails = new JsonObject();

        String defaultCardType = String.valueOf(CREDIT);
        String defaultWalletType = "APPLE_PAY";

        transactionDetails.addProperty("credential_external_id", credentialExternalId);
        transactionDetails.addProperty("language", language);
        transactionDetails.addProperty("return_url", returnUrl);
        transactionDetails.addProperty("payment_provider", paymentProvider);
        transactionDetails.addProperty("delayed_capture", delayedCapture);
        transactionDetails.addProperty("gateway_transaction_id", gatewayTransactionId);
        transactionDetails.addProperty("corporate_surcharge", corporateCardSurcharge);
        transactionDetails.addProperty("refunded_by", refundedById);
        transactionDetails.addProperty("user_email", refundedByUserEmail);
        transactionDetails.addProperty("card_type", defaultCardType);
        transactionDetails.addProperty("wallet", defaultWalletType);
        transactionDetails.addProperty("authorisation_mode", authorisationMode.getName());

        if ("REFUND".equals(transactionType) || "DISPUTE".equals(transactionType)) {
            refundPaymentDetails.addProperty("card_brand_label", cardBrandLabel);
            refundPaymentDetails.addProperty("expiry_date", cardExpiryDate);
            refundPaymentDetails.addProperty("card_type", defaultCardType);
            refundPaymentDetails.addProperty("wallet", defaultWalletType);
            transactionDetails.add("payment_details", refundPaymentDetails);
        }

        Optional.ofNullable(cardBrandLabel)
                .ifPresent(cardBrandLabel -> transactionDetails.addProperty("card_brand_label", cardBrandLabel));
        Optional.ofNullable(externalMetadata)
                .ifPresent(cd -> transactionDetails.add("external_metadata", externalMetadata));
        Optional.ofNullable(captureSubmittedDate).ifPresent(
                date -> transactionDetails.addProperty("capture_submitted_date", date.toString())
        );
        Optional.ofNullable(capturedDate).ifPresent(
                date -> transactionDetails.addProperty("captured_date", date.toString())
        );
        Optional.ofNullable(cardDetails)
                .ifPresent(cd -> {
                    transactionDetails.addProperty("expiry_date", cardExpiryDate);
                    Optional.ofNullable(cd.getBillingAddress())
                            .ifPresent(ba -> {
                                transactionDetails.addProperty("address_line1", ba.getAddressLine1());
                                transactionDetails.addProperty("address_line2", ba.getAddressLine2());
                                transactionDetails.addProperty("address_postcode", ba.getAddressPostCode());
                                transactionDetails.addProperty("address_city", ba.getAddressCity());
                                transactionDetails.addProperty("address_county", ba.getAddressCounty());
                                transactionDetails.addProperty("address_country", ba.getAddressCountry());
                            });
                });
        if(exemption3dsRequested != null) {
            transactionDetails.addProperty("exemption_3ds_requested", exemption3dsRequested.name());
        }
        if(exemption3ds != null) {
            transactionDetails.addProperty("exemption3ds", exemption3ds.name());
        }
        Optional.ofNullable(version3ds).ifPresent(
                version -> {
                    transactionDetails.addProperty("version_3ds", version);
                    transactionDetails.addProperty("requires_3ds", requires3ds);
                });
        Optional.ofNullable(disputed).ifPresent(
                disputed -> transactionDetails.addProperty("disputed", disputed)
        );
        Optional.ofNullable(canRetry).ifPresent(
                canRetry -> transactionDetails.addProperty("can_retry", canRetry)
        );
        return transactionDetails;
    }

    @Override
    public TransactionEntity toEntity() {
        var builder = new TransactionEntity.Builder()
                .withId(id)
                .withServiceId(serviceId)
                .withGatewayAccountId(gatewayAccountId)
                .withExternalId(externalId)
                .withParentExternalId(parentExternalId)
                .withAmount(amount)
                .withReference(reference)
                .withDescription(description)
                .withState(state)
                .withEmail(email)
                .withCardholderName(cardholderName)
                .withCreatedDate(createdDate)
                .withTransactionDetails(transactionDetails)
                .withEventCount(eventCount)
                .withCardBrand(cardBrand)
                .withLastDigitsCardNumber(lastDigitsCardNumber)
                .withFirstDigitsCardNumber(firstDigitsCardNumber)
                .withNetAmount(netAmount)
                .withTotalAmount(totalAmount)
                .withRefundStatus(refundStatus)
                .withRefundAmountRefunded(refundAmountRefunded)
                .withRefundAmountAvailable(refundAmountAvailable)
                .withFee(fee)
                .withTransactionType(transactionType)
                .withLive(live)
                .withMoto(moto)
                .withGatewayTransactionId(gatewayTransactionId)
                .withGatewayPayoutId(gatewayPayoutId)
                .withAgreementId(agreementId);
        Source.from(source).ifPresent(builder::withSource);
        return builder.build();
    }

    public String getExternalId() {
        return externalId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public boolean isLive() {
        return live;
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

    public String getParentExternalId() {
        return parentExternalId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public Long getRefundAmountAvailable() {
        return refundAmountAvailable;
    }

    public Long getRefundAmountRefunded() {
        return refundAmountRefunded;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public String getGatewayPayoutId() {
        return gatewayPayoutId;
    }

    public String getCredentialExternalId() {
        return credentialExternalId;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public TransactionFixture withDefaultCardDetails(boolean includeCardDetails) {
        if (includeCardDetails) {
            Address billingAddress = new Address("line1", "line2", "AB1 2CD",
                    "London", null, "GB");

            cardDetails = new CardDetails(cardholderName, billingAddress, cardBrand,
                    "1234", "123456", cardExpiryDate, CREDIT);
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

    public TransactionFixture withFee(Long value) {
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
        this.refundAmountRefunded = refundSummary.getAmountRefunded();
        return this;
    }

    public TransactionFixture withTotalAmount(long value) {
        this.totalAmount = value;
        return this;
    }

    public TransactionFixture withCaptureSubmittedDate(ZonedDateTime time) {
        this.captureSubmittedDate = time;
        return this;
    }

    public TransactionFixture withCapturedDate(ZonedDateTime time) {
        this.capturedDate = time;
        return this;
    }

    public TransactionFixture withRefundedById(String refundedById) {
        this.refundedById = refundedById;
        return this;
    }

    public TransactionFixture withRefundedByUserEmail(String refundedByUserEmail) {
        this.refundedByUserEmail = refundedByUserEmail;
        return this;
    }

    public TransactionFixture withDefaultPaymentDetails() {
        // default values are already assigned for cardBrand, cardholderName, reference, description, email

        this.firstDigitsCardNumber = "123456";
        this.lastDigitsCardNumber = "1234";
        this.cardBrandLabel = "Visa";
        return this;
    }
}
