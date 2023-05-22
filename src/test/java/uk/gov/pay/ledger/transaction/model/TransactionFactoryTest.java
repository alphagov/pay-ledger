package uk.gov.pay.ledger.transaction.model;

import com.google.gson.JsonObject;
import io.dropwizard.jackson.Jackson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.service.payments.commons.model.AuthorisationMode;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.pay.ledger.payout.entity.PayoutEntity.PayoutEntityBuilder.aPayoutEntity;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class TransactionFactoryTest {

    private TransactionFactory transactionFactory;
    private TransactionEntity fullDataObject;
    private TransactionEntity minimalDataObject;

    private Long id = 1L;
    private String gatewayAccountId = "ruwe2424";
    private String serviceId = "test_service_id";
    private Boolean live = true;
    private String externalId = "ch_q72347235";
    private Long amount = 100L;
    private String reference = "ref_237156782465";
    private String description = "test description";
    private TransactionState state = TransactionState.valueOf("CREATED");
    private String email = "test@email.com";
    private String cardholderName = "M Jan Kowalski";
    private ZonedDateTime createdDate = ZonedDateTime.now();
    private JsonObject fullTransactionDetails = new JsonObject();
    private Integer eventCount = 2;
    private String cardBrand = "visa";
    private String lastDigitsCardNumber = "5678";
    private String firstDigitsCardNumber = "123456";
    private Long netAmount = 77L;
    private Long totalAmount = 99L;
    private String refundStatus = "available";
    private Long refundAmountRefunded = 0L;
    private Long refundAmountAvailable = 99L;
    private Long fee = 66L;
    private String cardExpiryDate = "10/27";
    private String walletType = "APPLE_PAY";
    private ZonedDateTime paidOutDate = ZonedDateTime.parse("2017-09-19T08:46:01.123456Z");
    private Boolean canRetry = false;

    @BeforeEach
    public void setUp() {
        JsonObject metadata = new JsonObject();
        metadata.addProperty("ledger_code", 123);
        metadata.addProperty("some_key", "key");

        fullTransactionDetails.addProperty("credential_external_id", "credential-external-id");
        fullTransactionDetails.addProperty("language", "en");
        fullTransactionDetails.addProperty("return_url", "https://test.url.com");
        fullTransactionDetails.addProperty("payment_provider", "sandbox");
        fullTransactionDetails.addProperty("delayed_capture", true);
        fullTransactionDetails.addProperty("gateway_transaction_id", "gti_12334");
        fullTransactionDetails.addProperty("corporate_surcharge", 12);
        fullTransactionDetails.addProperty("fee", 5);
        fullTransactionDetails.addProperty("card_brand_label", "Visa");
        fullTransactionDetails.addProperty("address_line1", "line 1");
        fullTransactionDetails.addProperty("address_line2", "line 2");
        fullTransactionDetails.addProperty("address_postcode", "A11 11BB");
        fullTransactionDetails.addProperty("address_line1", "line 1");
        fullTransactionDetails.addProperty("address_city", "London");
        fullTransactionDetails.addProperty("address_county", "London");
        fullTransactionDetails.addProperty("address_country", "GB");
        fullTransactionDetails.addProperty("capture_submitted_date", "2017-09-09T09:35:45.695951+01");
        fullTransactionDetails.addProperty("captured_date", "2017-09-09T09:35:45.695951+01");
        fullTransactionDetails.add("external_metadata", metadata);
        fullTransactionDetails.addProperty("expiry_date", cardExpiryDate);
        fullTransactionDetails.addProperty("wallet", walletType);
        fullTransactionDetails.addProperty("authorisation_mode", "moto_api");
        fullTransactionDetails.addProperty("disputed", true);
        fullTransactionDetails.addProperty("canRetry", false);

        var payoutObject = aPayoutEntity()
                .withPaidOutDate(paidOutDate)
                .build();

        fullDataObject = new TransactionEntity.Builder()
                .withTransactionType("PAYMENT")
                .withId(id)
                .withGatewayAccountId(gatewayAccountId)
                .withServiceId(serviceId)
                .withLive(live)
                .withExternalId(externalId)
                .withAmount(amount)
                .withReference(reference)
                .withDescription(description)
                .withState(state)
                .withEmail(email)
                .withCardholderName(cardholderName)
                .withCreatedDate(createdDate)
                .withTransactionDetails(fullTransactionDetails.toString())
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
                .withPayoutEntity(payoutObject)
                .build();

        minimalDataObject = new TransactionEntity.Builder()
                .withTransactionType("PAYMENT")
                .withId(id)
                .withGatewayAccountId(gatewayAccountId)
                .withServiceId(serviceId)
                .withLive(live)
                .withExternalId(externalId)
                .withAmount(amount)
                .withReference(reference)
                .withDescription(description)
                .withState(state)
                .withEmail(email)
                .withCardholderName(cardholderName)
                .withCreatedDate(createdDate)
                .withEventCount(eventCount)
                .withCardBrand(cardBrand)
                .withLastDigitsCardNumber(lastDigitsCardNumber)
                .withFirstDigitsCardNumber(firstDigitsCardNumber)
                .withNetAmount(netAmount)
                .withTotalAmount(totalAmount)
                .withRefundStatus(refundStatus)
                .withRefundAmountRefunded(refundAmountRefunded)
                .withRefundAmountAvailable(refundAmountAvailable)
                .build();

        transactionFactory = new TransactionFactory(Jackson.newObjectMapper());
    }

    @Test
    public void createsPaymentFromTransactionEntityWithFullData() {
        Payment payment = (Payment) transactionFactory.createTransactionEntity(fullDataObject);

        assertCorrectPaymentTransactionWithFullData(payment);
    }

    @Test
    public void createsPaymentFromTransactionEntityWithMinimalData() {
        Payment payment = (Payment) transactionFactory.createTransactionEntity(minimalDataObject);

        assertThat(payment.getGatewayAccountId(), is(gatewayAccountId));
        assertThat(payment.getServiceId(), is(serviceId));
        assertThat(payment.getLive(), is(live));
        assertThat(payment.getAmount(), is(amount));
        assertThat(payment.getExternalId(), is(externalId));
        assertThat(payment.getReference(), is(reference));
        assertThat(payment.getDescription(), is(description));
        assertThat(payment.getState(), is(state));
        assertThat(payment.getLanguage(), nullValue());
        assertThat(payment.getReturnUrl(), nullValue());
        assertThat(payment.getEmail(), is(email));
        assertThat(payment.getPaymentProvider(), nullValue());
        assertThat(payment.getCreatedDate(), is(createdDate));
        assertThat(payment.getCardDetails(), notNullValue());
        assertThat(payment.getCardDetails().getLastDigitsCardNumber(), is(lastDigitsCardNumber));
        assertThat(payment.getCardDetails().getFirstDigitsCardNumber(), is(firstDigitsCardNumber));
        assertThat(payment.getCardDetails().getCardHolderName(), is(cardholderName));
        assertThat(payment.getCardDetails().getCardBrand(), emptyString());
        assertThat(payment.getCardDetails().getBillingAddress(), nullValue());
        assertThat(payment.getDelayedCapture(), is(false));
        assertThat(payment.getExternalMetadata(), nullValue());
        assertThat(payment.getEventCount(), is(eventCount));
        assertThat(payment.getGatewayTransactionId(), nullValue());
        assertThat(payment.getCorporateCardSurcharge(), nullValue());
        assertThat(payment.getFee(), nullValue());
        assertThat(payment.getNetAmount(), is(netAmount));
        assertThat(payment.getTotalAmount(), is(totalAmount));
        assertThat(payment.getRefundSummary(), notNullValue());
        assertThat(payment.getRefundSummary().getStatus(), is(refundStatus));
        assertThat(payment.getRefundSummary().getAmountAvailable(), is(refundAmountAvailable));
        assertThat(payment.getRefundSummary().getAmountRefunded(), is(refundAmountRefunded));
        assertThat(payment.getSettlementSummary(), notNullValue());
        assertThat(payment.getSettlementSummary().getCapturedDate(), is(Optional.empty()));
        assertThat(payment.getSettlementSummary().getSettlementSubmittedTime(), is(Optional.empty()));
        assertThat(payment.getSettlementSummary().getSettledDate(), is(Optional.empty()));
        assertThat(payment.getAuthorisationMode(), is(AuthorisationMode.WEB));
        assertThat(payment.getDisputed(), is(false));
        assertThat(payment.getCanRetry(), is(nullValue()));
    }

    @Test
    public void createsRefundFromTransactionEntityWithMinimalData() {
        TransactionEntity refund = new TransactionEntity.Builder()
                .withTransactionType("REFUND")
                .withId(id)
                .withGatewayAccountId(gatewayAccountId)
                .withServiceId(serviceId)
                .withLive(live)
                .withExternalId(externalId)
                .withParentExternalId("parent-ext-id")
                .withAmount(amount)
                .withGatewayTransactionId("gti_12334")
                .withState(state)
                .withCreatedDate(createdDate)
                .withEventCount(eventCount)
                .withTransactionDetails("{\"refunded_by\": \"some_user_id\", \"user_email\": \"test@example.com\"}")
                .build();
        Refund refundEntity = (Refund) transactionFactory.createTransactionEntity(refund);

        assertThat(refundEntity.getGatewayAccountId(), is(gatewayAccountId));
        assertThat(refundEntity.getServiceId(), is(serviceId));
        assertThat(refundEntity.getLive(), is(live));
        assertThat(refundEntity.getAmount(), is(amount));
        assertThat(refundEntity.getExternalId(), is(externalId));
        assertThat(refundEntity.getParentExternalId(), is("parent-ext-id"));
        assertThat(refundEntity.getRefundedBy(), is("some_user_id"));
        assertThat(refundEntity.getRefundedByUserEmail(), is("test@example.com"));
        assertThat(refundEntity.getGatewayTransactionId(), is("gti_12334"));
        assertThat(refundEntity.getState(), is(state));
        assertThat(refundEntity.getCreatedDate(), is(createdDate));
        assertThat(refundEntity.getEventCount(), is(eventCount));
    }

    @Test
    public void createsRefundWithSharedPaymentDetailsFromTransactionEntity() {
        TransactionEntity refund = new TransactionEntity.Builder()
                .withTransactionType("REFUND")
                .withId(id)
                .withGatewayAccountId(gatewayAccountId)
                .withExternalId(externalId)
                .withParentExternalId("parent-ext-id")
                .withAmount(amount)
                .withGatewayTransactionId("gti_12334")
                .withState(state)
                .withCreatedDate(createdDate)
                .withEventCount(eventCount)
                .withTransactionDetails("{\"refunded_by\": \"some_user_id\", \"user_email\": \"test@example.com\", \"payment_details\": {\"expiry_date\": \"10/27\", \"card_type\": \"credit\", \"wallet\": \"APPLE_PAY\", \"card_brand_label\": \"Visa\"}}")
                .withCardholderName("a-cardholder-name")
                .withFirstDigitsCardNumber("1234")
                .withLastDigitsCardNumber("5678")
                .withEmail("a-email")
                .withDescription("a-description")
                .withReference("a-reference")
                .build();
        Refund refundEntity = (Refund) transactionFactory.createTransactionEntity(refund);

        assertThat(refundEntity.getGatewayAccountId(), is(gatewayAccountId));
        assertThat(refundEntity.getAmount(), is(amount));
        assertThat(refundEntity.getExternalId(), is(externalId));
        assertThat(refundEntity.getParentExternalId(), is("parent-ext-id"));
        assertThat(refundEntity.getRefundedBy(), is("some_user_id"));
        assertThat(refundEntity.getRefundedByUserEmail(), is("test@example.com"));
        assertThat(refundEntity.getGatewayTransactionId(), is("gti_12334"));
        assertThat(refundEntity.getState(), is(state));
        assertThat(refundEntity.getCreatedDate(), is(createdDate));
        assertThat(refundEntity.getEventCount(), is(eventCount));

        assertThat(refundEntity.getPaymentDetails().getCardDetails().getCardHolderName(), is("a-cardholder-name"));
        assertThat(refundEntity.getPaymentDetails().getEmail(), is("a-email"));
        assertThat(refundEntity.getPaymentDetails().getDescription(), is("a-description"));

        assertThat(refundEntity.getPaymentDetails().getCardDetails().getCardBrand(), is("Visa"));

        assertThat(refundEntity.getPaymentDetails().getCardDetails().getLastDigitsCardNumber(), is("5678"));
        assertThat(refundEntity.getPaymentDetails().getCardDetails().getFirstDigitsCardNumber(), is("1234"));

        assertThat(refundEntity.getPaymentDetails().getReference(), is("a-reference"));
        assertThat(refundEntity.getPaymentDetails().getCardDetails().getExpiryDate(), is("10/27"));
        assertThat(refundEntity.getPaymentDetails().getCardDetails().getCardType(), is(CardType.CREDIT));
        assertThat(refundEntity.getPaymentDetails().getWalletType(), is("APPLE_PAY"));
    }

    @Test
    public void createsRefundWithSettlementSummaryFromTransactionEntity() {
        var payoutObject = aPayoutEntity()
                .withPaidOutDate(paidOutDate)
                .build();
        var refund = new TransactionEntity.Builder()
                .withTransactionType(TransactionType.REFUND.name())
                .withTransactionDetails(fullTransactionDetails.toString())
                .withPayoutEntity(payoutObject)
                .build();
        assertThat(refund.getPayoutEntity().isPresent(), is(true));
        assertThat(refund.getPayoutEntity().get().getPaidOutDate(), is(notNullValue()));
    }

    @Test
    public void createsDispute() {
        var createdDate = ZonedDateTime.parse("2022-06-08T11:22:48.822408Z");
        var paidOutDate = ZonedDateTime.parse("2022-07-08T12:20:07.073Z");
        var evidenceDueDate = "2022-05-10T22:59:59.000000Z";
        TransactionEntity parentTransactionEntity = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withDefaultCardDetails()
                .withDefaultTransactionDetails()
                .withGatewayAccountId("1")
                .withExternalId("blabla")
                .toEntity();

        PayoutEntity payoutEntity = aPayoutEntity()
                .withGatewayAccountId(parentTransactionEntity.getGatewayAccountId())
                .withGatewayPayoutId("po_dl0e0sdlejskfklsele")
                .withPaidOutDate(paidOutDate)
                .build();

        TransactionEntity.Builder transactionEntityBuilder = new TransactionEntity.Builder();
        TransactionEntity disputeTransactionEntity = transactionEntityBuilder
                .withGatewayAccountId(parentTransactionEntity.getGatewayAccountId())
                .withParentExternalId(parentTransactionEntity.getExternalId())
                .withReference(parentTransactionEntity.getReference())
                .withDescription(parentTransactionEntity.getDescription())
                .withEmail(parentTransactionEntity.getEmail())
                .withCardholderName(parentTransactionEntity.getCardholderName())
                .withGatewayAccountId(parentTransactionEntity.getGatewayAccountId())
                .withTransactionType("DISPUTE")
                .withState(TransactionState.LOST)
                .withAmount(1000L)
                .withNetAmount(-2500L)
                .withGatewayTransactionId("gateway-transaction-id")
                .withTransactionDetails("{\"amount\": 1000, \"payment_details\": {\"card_type\": \"CREDIT\", \"expiry_date\": \"11/23\", \"card_brand_label\": \"Visa\"}, \"gateway_account_id\": \"1\", \"gateway_transaction_id\": \"du_dl20kdldj20ejs103jns\", \"reason\": \"fraudulent\", \"evidence_due_date\": \"" + evidenceDueDate + "\"}")
                .withEventCount(3)
                .withCardBrand(parentTransactionEntity.getCardBrand())
                .withFee(1500L)
                .withGatewayTransactionId("du_dl20kdldj20ejs103jns")
                .withServiceId(parentTransactionEntity.getServiceId())
                .withGatewayPayoutId("po_dl0e0sdlejskfklsele")
                .withCreatedDate(createdDate)
                .withPayoutEntity(payoutEntity)
                .withLive(true)
                .build();

        Dispute transaction = (Dispute) transactionFactory.createTransactionEntity(disputeTransactionEntity);

        assertThat(transaction.getReason(), is("fraudulent"));
        assertThat(transaction.getParentTransactionId(), is(parentTransactionEntity.getExternalId()));
        assertThat(transaction.getEvidenceDueDate(), is(ZonedDateTime.parse(evidenceDueDate)));
        assertThat(transaction.getNetAmount(), is(-2500L));
        assertThat(transaction.getSettlementSummary().getSettledDate().isPresent(), is(true));
        assertThat(transaction.getSettlementSummary().getSettledDate(), is(Optional.of("2022-07-08")));
        assertThat(transaction.getPaymentDetails().getDescription(), is(parentTransactionEntity.getDescription()));
    }

    private void assertCorrectPaymentTransactionWithFullData(Payment payment) {
        assertThat(payment.getGatewayAccountId(), is(gatewayAccountId));
        assertThat(payment.getServiceId(), is(serviceId));
        assertThat(payment.getLive(), is(live));
        assertThat(payment.getCredentialExternalId(), is("credential-external-id"));
        assertThat(payment.getAmount(), is(amount));
        assertThat(payment.getExternalId(), is(externalId));
        assertThat(payment.getReference(), is(reference));
        assertThat(payment.getDescription(), is(description));
        assertThat(payment.getState(), is(state));
        assertThat(payment.getLanguage(), is("en"));
        assertThat(payment.getReturnUrl(), is("https://test.url.com"));
        assertThat(payment.getEmail(), is(email));
        assertThat(payment.getPaymentProvider(), is("sandbox"));
        assertThat(payment.getCreatedDate(), is(createdDate));
        assertThat(payment.getCardDetails(), notNullValue());
        assertThat(payment.getCardDetails().getExpiryDate(), is(cardExpiryDate));
        assertThat(payment.getCardDetails().getLastDigitsCardNumber(), is(lastDigitsCardNumber));
        assertThat(payment.getCardDetails().getFirstDigitsCardNumber(), is(firstDigitsCardNumber));
        assertThat(payment.getCardDetails().getCardHolderName(), is(cardholderName));
        assertThat(payment.getCardDetails().getCardBrand(), is("Visa"));
        assertThat(payment.getCardDetails().getBillingAddress(), notNullValue());
        assertThat(payment.getCardDetails().getBillingAddress().getAddressLine1(), is("line 1"));
        assertThat(payment.getCardDetails().getBillingAddress().getAddressLine2(), is("line 2"));
        assertThat(payment.getCardDetails().getBillingAddress().getAddressPostCode(), is("A11 11BB"));
        assertThat(payment.getCardDetails().getBillingAddress().getAddressCity(), is("London"));
        assertThat(payment.getCardDetails().getBillingAddress().getAddressCounty(), is("London"));
        assertThat(payment.getCardDetails().getBillingAddress().getAddressCountry(), is("GB"));
        assertThat(payment.getDelayedCapture(), is(true));
        assertThat(payment.getExternalMetadata(), is(ImmutableMap.of("ledger_code", 123, "some_key", "key")));
        assertThat(payment.getEventCount(), is(eventCount));
        assertThat(payment.getGatewayTransactionId(), is("gti_12334"));
        assertThat(payment.getCorporateCardSurcharge(), is(12L));
        assertThat(payment.getFee(), is(fee));
        assertThat(payment.getNetAmount(), is(netAmount));
        assertThat(payment.getTotalAmount(), is(totalAmount));
        assertThat(payment.getRefundSummary(), notNullValue());
        assertThat(payment.getRefundSummary().getStatus(), is(refundStatus));
        assertThat(payment.getRefundSummary().getAmountAvailable(), is(refundAmountAvailable));
        assertThat(payment.getRefundSummary().getAmountRefunded(), is(refundAmountRefunded));
        assertThat(payment.getSettlementSummary(), notNullValue());
        assertThat(payment.getSettlementSummary().getCapturedDate(), is(Optional.of("2017-09-09")));
        assertThat(payment.getSettlementSummary().getSettlementSubmittedTime(), is(Optional.of("2017-09-09T08:35:45.695Z")));
        assertThat(payment.getSettlementSummary().getSettledDate(), is(Optional.of("2017-09-19")));
        assertThat(payment.getWalletType(), is(walletType));
        assertThat(payment.getAuthorisationMode(), is(AuthorisationMode.MOTO_API));
        assertThat(payment.getDisputed(), is(true));
        assertThat(payment.getCanRetry(), is(Boolean.FALSE));
    }
}
