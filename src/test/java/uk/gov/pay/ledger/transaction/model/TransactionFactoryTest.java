package uk.gov.pay.ledger.transaction.model;

import com.google.gson.JsonObject;
import io.dropwizard.jackson.Jackson;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.emptyString;

public class TransactionFactoryTest {

    private TransactionFactory transactionFactory;
    private TransactionEntity fullDataObject;
    private TransactionEntity minimalDataObject;

    private Long id = 1L;
    private String gatewayAccountId = "ruwe2424";
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

    @Before
    public void setUp() {
        JsonObject metadata = new JsonObject();
        metadata.addProperty("ledger_code", 123);
        metadata.addProperty("some_key", "key");

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


        fullDataObject = new TransactionEntity.Builder()
                .withId(id)
                .withGatewayAccountId(gatewayAccountId)
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
                .build();

        minimalDataObject = new TransactionEntity.Builder()
                .withId(id)
                .withGatewayAccountId(gatewayAccountId)
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
    }

    @Test
    public void createsRefundFromTransactionEntityWithMinimalData() {
        TransactionEntity refund = new TransactionEntity.Builder()
                .withTransactionType("REFUND")
                .withId(id)
                .withGatewayAccountId(gatewayAccountId)
                .withExternalId(externalId)
                .withParentExternalId("parent-ext-id")
                .withAmount(amount)
                .withReference(reference)
                .withState(state)
                .withCreatedDate(createdDate)
                .withEventCount(eventCount)
                .withParentTransactionEntity(fullDataObject)
                .withTransactionDetails("{\"refunded_by\": \"some_user_id\", \"user_email\": \"test@example.com\"}")
                .build();
        Refund refundEntity = (Refund) transactionFactory.createTransactionEntity(refund);

        assertThat(refundEntity.getGatewayAccountId(), is(gatewayAccountId));
        assertThat(refundEntity.getAmount(), is(amount));
        assertThat(refundEntity.getExternalId(), is(externalId));
        assertThat(refundEntity.getParentExternalId(), is("parent-ext-id"));
        assertThat(refundEntity.getRefundedBy(), is("some_user_id"));
        assertThat(refundEntity.getRefundedByUserEmail(), is("test@example.com"));
        assertThat(refundEntity.getReference(), is(reference));
        assertThat(refundEntity.getState(), is(state));
        assertThat(refundEntity.getCreatedDate(), is(createdDate));
        assertThat(refundEntity.getEventCount(), is(eventCount));

        assertCorrectPaymentTransactionWithFullData((Payment) refundEntity.getParentTransaction().get());
    }

    private void assertCorrectPaymentTransactionWithFullData(Payment payment) {
        assertThat(payment.getGatewayAccountId(), is(gatewayAccountId));
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
        assertThat(payment.getWalletType(), is(walletType));
    }
}