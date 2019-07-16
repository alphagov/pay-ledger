package uk.gov.pay.ledger.transaction.model;

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

public class PaymentFactoryTest {

    private PaymentFactory paymentFactory;
    private TransactionEntity fullDataObject;
    private TransactionEntity minimalDataObject;

    private Long id = 1L;
    private String gatewayAccountId = "ruwe2424";
    private String externalId = "ch_q72347235";
    private Long amount = 100L;
    private String reference = "ref_237156782465";
    private String description = "test description";
    private String state = "created";
    private String email = "test@email.com";
    private String cardholderName = "M Jan Kowalski";
    private String fullExternalMetadata = "{\"ledger_code\":123, \"some_key\":\"key\"}";
    private ZonedDateTime createdDate = ZonedDateTime.now();
    private String fullTransactionDetails = "{\n" +
            "  \"language\": \"en\",\n" +
            "  \"return_url\": \"https://test.url.com\",\n" +
            "  \"payment_provider\": \"sandbox\",\n" +
            "  \"delayed_capture\": true,\n" +
            "  \"gateway_transaction_id\": \"gti_12334\",\n" +
            "  \"corporate_surcharge\": 12,\n" +
            "  \"fee\": 5,\n" +
            "  \"address_line1\": \"line 1\",\n" +
            "  \"address_line2\": \"line 2\",\n" +
            "  \"address_postcode\": \"A11 11BB\",\n" +
            "  \"address_city\": \"London\",\n" +
            "  \"address_county\": \"London\",\n" +
            "  \"address_country\": \"GB\"\n" +
            "}";
    private Integer eventCount = 2;
    private String cardBrand = "visa";
    private String lastDigitsCardNumber = "5678";
    private String firstDigitsCardNumber = "123456";
    private Long netAmount = 77L;
    private Long totalAmount = 99L;
    private ZonedDateTime settlementSubmittedTime = ZonedDateTime.parse("2017-09-09T09:35:45.695951+01");
    private ZonedDateTime settledTime = ZonedDateTime.parse("2017-09-09T12:13Z");
    private String refundStatus = "available";
    private Long refundAmountSubmitted = 0L;
    private Long refundAmountAvailable = 99L;

    @Before
    public void setUp() {
        fullDataObject = new TransactionEntity(id, gatewayAccountId, externalId, amount, reference, description, state,
                email, cardholderName, fullExternalMetadata, createdDate, fullTransactionDetails, eventCount, cardBrand,
                lastDigitsCardNumber, firstDigitsCardNumber, netAmount, totalAmount, settlementSubmittedTime, settledTime,
                refundStatus, refundAmountSubmitted, refundAmountAvailable);

        minimalDataObject = new TransactionEntity(id, gatewayAccountId, externalId, amount, reference, description, state,
                email, cardholderName, null, createdDate, null, eventCount, cardBrand,
                lastDigitsCardNumber, firstDigitsCardNumber, netAmount, totalAmount, settlementSubmittedTime,
                settledTime, refundStatus, refundAmountSubmitted, refundAmountAvailable);

        paymentFactory = new PaymentFactory(Jackson.newObjectMapper());
    }

    @Test
    public void createsPaymentFromTransactionEntityWithFullData() {
        var payment = paymentFactory.createTransactionEntity(fullDataObject);

        assertThat(payment.getGatewayAccountId(), is(gatewayAccountId));
        assertThat(payment.getAmount(), is(amount));
        assertThat(payment.getExternalId(), is(externalId));
        assertThat(payment.getReference(), is(reference));
        assertThat(payment.getDescription(), is(description));
        assertThat(payment.getState(), is(TransactionState.from(state)));
        assertThat(payment.getLanguage(), is("en"));
        assertThat(payment.getReturnUrl(), is("https://test.url.com"));
        assertThat(payment.getEmail(), is(email));
        assertThat(payment.getPaymentProvider(), is("sandbox"));
        assertThat(payment.getCreatedDate(), is(createdDate));
        assertThat(payment.getCardDetails(), notNullValue());
        assertThat(payment.getCardDetails().getLastDigitsCardNumber(), is(lastDigitsCardNumber));
        assertThat(payment.getCardDetails().getFirstDigitsCardNumber(), is(firstDigitsCardNumber));
        assertThat(payment.getCardDetails().getCardHolderName(), is(cardholderName));
        assertThat(payment.getCardDetails().getCardBrand(), is(cardBrand));
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
        assertThat(payment.getFee(), is(5L));
        assertThat(payment.getNetAmount(), is(netAmount));
        assertThat(payment.getTotalAmount(), is(totalAmount));
        assertThat(payment.getRefundSummary(), notNullValue());
        assertThat(payment.getRefundSummary().getStatus(), is(refundStatus));
        assertThat(payment.getRefundSummary().getAmountAvailable(), is(refundAmountAvailable));
        assertThat(payment.getRefundSummary().getAmountSubmitted(), is(refundAmountSubmitted));
        assertThat(payment.getSettlementSummary(), notNullValue());
        assertThat(payment.getSettlementSummary().getCapturedDate(), is(Optional.of("2017-09-09")));
        assertThat(payment.getSettlementSummary().getSettlementSubmittedTime(), is(Optional.of("2017-09-09T08:35:45.695Z")));
    }

    @Test
    public void createsPaymentFromTransactionEntityWithMinimalData() {
        var payment = paymentFactory.createTransactionEntity(minimalDataObject);

        assertThat(payment.getGatewayAccountId(), is(gatewayAccountId));
        assertThat(payment.getAmount(), is(amount));
        assertThat(payment.getExternalId(), is(externalId));
        assertThat(payment.getReference(), is(reference));
        assertThat(payment.getDescription(), is(description));
        assertThat(payment.getState(), is(TransactionState.from(state)));
        assertThat(payment.getLanguage(), nullValue());
        assertThat(payment.getReturnUrl(), nullValue());
        assertThat(payment.getEmail(), is(email));
        assertThat(payment.getPaymentProvider(), nullValue());
        assertThat(payment.getCreatedDate(), is(createdDate));
        assertThat(payment.getCardDetails(), notNullValue());
        assertThat(payment.getCardDetails().getLastDigitsCardNumber(), is(lastDigitsCardNumber));
        assertThat(payment.getCardDetails().getFirstDigitsCardNumber(), is(firstDigitsCardNumber));
        assertThat(payment.getCardDetails().getCardHolderName(), is(cardholderName));
        assertThat(payment.getCardDetails().getCardBrand(), is(cardBrand));
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
        assertThat(payment.getRefundSummary().getAmountSubmitted(), is(refundAmountSubmitted));
        assertThat(payment.getSettlementSummary(), notNullValue());
        assertThat(payment.getSettlementSummary().getCapturedDate(), is(Optional.of("2017-09-09")));
        assertThat(payment.getSettlementSummary().getSettlementSubmittedTime(), is(Optional.of("2017-09-09T08:35:45.695Z")));
    }
}