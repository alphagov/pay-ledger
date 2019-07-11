package uk.gov.pay.ledger.transaction.model;

import io.dropwizard.jackson.Jackson;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZonedDateTime;

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
            "  \"net_amount\": 77,\n" +
            "  \"total_amount\": 199,\n" +
            "  \"links\": [\n" +
            "    {\n" +
            "      \"href\": \"www.test-something.co.uk\",\n" +
            "      \"rel\": \"next_url\",\n" +
            "      \"method\": \"GET\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"refund_summary\": {\n" +
            "    \"status\": \"available\",\n" +
            "    \"amount_available\": 111,\n" +
            "    \"amount_submitted\": 0\n" +
            "  },\n" +
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

    @Before
    public void setUp() {
        fullDataObject = new TransactionEntity(id, gatewayAccountId, externalId, amount, reference, description, state,
                email, cardholderName, fullExternalMetadata, createdDate, fullTransactionDetails, eventCount, cardBrand,
                lastDigitsCardNumber, firstDigitsCardNumber);

        minimalDataObject = new TransactionEntity(id, gatewayAccountId, externalId, amount, reference, description, state,
                email, cardholderName, null, createdDate, null, eventCount, cardBrand,
                lastDigitsCardNumber, firstDigitsCardNumber);

        paymentFactory = new PaymentFactory(Jackson.newObjectMapper());
    }

    @Test
    public void createsPaymentFromTransactionEntityWithFullData() {
        var payment = paymentFactory.fromTransactionEntity(fullDataObject);

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
        assertThat(payment.getNetAmount(), is(77L));
        assertThat(payment.getTotalAmount(), is(199L));
        assertThat(payment.getRefundSummary(), notNullValue());
        assertThat(payment.getRefundSummary().getStatus(), is("available"));
        assertThat(payment.getRefundSummary().getAmountAvailable(), is(111L));
        assertThat(payment.getRefundSummary().getAmountSubmitted(), is(0L));
    }

    @Test
    public void createsPaymentFromTransactionEntityWithMinimalData() {
        var payment = paymentFactory.fromTransactionEntity(minimalDataObject);

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
        assertThat(payment.getCardDetails().getBillingAddress(), notNullValue());
        assertThat(payment.getCardDetails().getBillingAddress().getAddressLine1(), nullValue());
        assertThat(payment.getCardDetails().getBillingAddress().getAddressLine2(), nullValue());
        assertThat(payment.getCardDetails().getBillingAddress().getAddressPostCode(), nullValue());
        assertThat(payment.getCardDetails().getBillingAddress().getAddressCity(), nullValue());
        assertThat(payment.getCardDetails().getBillingAddress().getAddressCounty(), nullValue());
        assertThat(payment.getCardDetails().getBillingAddress().getAddressCountry(), nullValue());
        assertThat(payment.getDelayedCapture(), is(false));
        assertThat(payment.getExternalMetadata(), nullValue());
        assertThat(payment.getEventCount(), is(eventCount));
        assertThat(payment.getGatewayTransactionId(), nullValue());
        assertThat(payment.getCorporateCardSurcharge(), nullValue());
        assertThat(payment.getFee(), nullValue());
        assertThat(payment.getNetAmount(), nullValue());
        assertThat(payment.getTotalAmount(), nullValue());
        assertThat(payment.getRefundSummary(), nullValue());
    }
}