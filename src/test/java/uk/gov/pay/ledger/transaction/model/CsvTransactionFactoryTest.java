package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

class CsvTransactionFactoryTest {

    private CsvTransactionFactory csvTransactionFactory;
    private TransactionFixture transactionFixture;

    @BeforeEach
    public void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        csvTransactionFactory = new CsvTransactionFactory(objectMapper);

        transactionFixture = aTransactionFixture()
                .withState(TransactionState.FAILED_REJECTED)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withAmount(100L)
                .withGatewayTransactionId("gateway-transaction-id")
                .withCreatedDate(ZonedDateTime.parse("2018-03-12T16:25:01.123456Z"))
                .withMoto(true)
                .withCorporateCardSurcharge(23)
                .withCardBrandLabel("Visa")
                .withDefaultCardDetails()
                .withDefaultTransactionDetails();
    }

    @Test
    void toMapShouldReturnMapWithCorrectCsvDataForPaymentTransaction() {

        TransactionEntity transactionEntity = transactionFixture
                .withFee(5L)
                .withTotalAmount(123L)
                .toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertPaymentDetails(csvDataMap, transactionEntity);

        assertThat(csvDataMap.get("Amount"), is("1.00"));
        assertThat(csvDataMap.get("GOV.UK Payment ID"), is(transactionEntity.getExternalId()));
        assertThat(csvDataMap.get("Provider ID"), is(transactionEntity.getGatewayTransactionId()));
        assertThat(csvDataMap.get("State"), is("Declined"));
        assertThat(csvDataMap.get("Finished"), is(true));
        assertThat(csvDataMap.get("Fee"), is("0.05"));
        assertThat(csvDataMap.get("Net"), is("0.00"));
        assertThat(csvDataMap.get("Error Code"), is("P0010"));
        assertThat(csvDataMap.get("Error Message"), is("Payment method rejected"));
        assertThat(csvDataMap.get("Date Created"), is("12 Mar 2018"));
        assertThat(csvDataMap.get("Time Created"), is("16:25:01"));
        assertThat(csvDataMap.get("Corporate Card Surcharge"), is("0.23"));
        assertThat(csvDataMap.get("Total Amount"), is("0.00"));
        assertThat(csvDataMap.get("MOTO"), is(true));
        assertThat(csvDataMap.get("Payment Provider"), is("sandbox"));
        assertThat(csvDataMap.get("3-D Secure Required"), is(nullValue()));
    }

    @Test
    void toMapShouldReturnMapWithCorrectCsvDataForRefundTransaction() {

        var transactionEntity = transactionFixture
                .withTransactionType(TransactionType.REFUND.name())
                .withState(TransactionState.ERROR)
                .withAmount(123L)
                .withTotalAmount(123L)
                .withRefundedByUserEmail("refundedbyuser@example.org")
                .withGatewayTransactionId("refund-gateway-transaction-id")
                .withRefundedByUserEmail("refundedbyuser@example.org")
                .withParentExternalId("parent-external-id")
                .withCorporateCardSurcharge(0)
                .withDefaultPaymentDetails()
                .withDefaultTransactionDetails()
                .toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertPaymentDetails(csvDataMap, transactionEntity);
        assertThat(csvDataMap.get("Amount"), is("-1.23"));
        assertThat(csvDataMap.get("Provider ID"), is(transactionFixture.getGatewayTransactionId()));
        assertThat(csvDataMap.get("GOV.UK Payment ID"), is(transactionFixture.getParentExternalId()));
        assertThat(csvDataMap.get("State"), is("Refund error"));
        assertThat(csvDataMap.get("Finished"), is(true));
        assertThat(csvDataMap.get("Error Code"), is("P0050"));
        assertThat(csvDataMap.get("Error Message"), is("Payment provider returned an error"));
        assertThat(csvDataMap.get("Date Created"), is("12 Mar 2018"));
        assertThat(csvDataMap.get("Time Created"), is("16:25:01"));
        assertThat(csvDataMap.get("Corporate Card Surcharge"), is(nullValue()));
        assertThat(csvDataMap.get("Total Amount"), is("-1.23"));
        assertThat(csvDataMap.get("Net"), is("-1.23"));
        assertThat(csvDataMap.get("MOTO"), is(nullValue()));
    }

    @Test
    void toMapShouldReturnMapWithCorrectCsvDataForDisputeTransaction() {
        TransactionEntity transactionEntity = transactionFixture
                .withTransactionType(TransactionType.DISPUTE.name())
                .withState(TransactionState.LOST)
                .withAmount(2000L)
                .withFee(1500L)
                .withNetAmount(-3500L)
                .withParentExternalId("parent-external-id")
                .withReference("ref-1")
                .withDescription("test description")
                .withDefaultPaymentDetails()
                .withDefaultTransactionDetails()
                .toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertPaymentDetails(csvDataMap, transactionEntity);
        assertThat(csvDataMap.get("Amount"), is("-20.00"));
        assertThat(csvDataMap.get("Net"), is("-35.00"));
        assertThat(csvDataMap.get("Fee"), is("15.00"));
        assertThat(csvDataMap.get("Provider ID"), is(transactionFixture.getGatewayTransactionId()));
        assertThat(csvDataMap.get("GOV.UK Payment ID"), is(transactionFixture.getParentExternalId()));
        assertThat(csvDataMap.get("State"), is("Dispute lost to customer"));
        assertThat(csvDataMap.get("Finished"), is(true));
        assertThat(csvDataMap.get("Date Created"), is("12 Mar 2018"));
        assertThat(csvDataMap.get("Time Created"), is("16:25:01"));

        assertThat(csvDataMap.get("Reference"), is("ref-1"));
        assertThat(csvDataMap.get("Description"), is("test description"));
        assertThat(csvDataMap.get("Card Number"), is("1234"));
        assertThat(csvDataMap.get("Card Brand"), is("Visa"));
        assertThat(csvDataMap.get("Cardholder Name"), is("J Doe"));
        assertThat(csvDataMap.get("Card Expiry Date"), is("10/21"));
    }

    @Test
    void toMapShouldUseAmountForTotalAmountForSuccessfulPayment() {
        TransactionEntity transactionEntity = transactionFixture
                .withState(TransactionState.SUCCESS)
                .withAmount(1500L)
                .toEntity();
        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertPaymentDetails(csvDataMap, transactionEntity);
        assertThat(csvDataMap.get("Total Amount"), is("15.00"));
    }

    @Test
    void toMapShouldUseZeroForTotalAmountForFailedPayment() {
        TransactionEntity transactionEntity = transactionFixture
                .withState(TransactionState.FAILED_REJECTED)
                .withAmount(1500L)
                .toEntity();
        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertPaymentDetails(csvDataMap, transactionEntity);
        assertThat(csvDataMap.get("Total Amount"), is("0.00"));
    }

    @Test
    void toMapShouldLeaveTotalAmountBlankForInProgressPayment() {
        TransactionEntity transactionEntity = transactionFixture
                .withState(TransactionState.STARTED)
                .withAmount(1500L)
                .toEntity();
        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertPaymentDetails(csvDataMap, transactionEntity);
        assertThat(csvDataMap.get("Total Amount"), is(""));
    }

    @Test
    void toMapShouldUseZeroForNetForFailedTransactionWhenNetUnavailable() {
        TransactionEntity transactionEntity = transactionFixture.withState(TransactionState.FAILED_CANCELLED).toEntity();
        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertPaymentDetails(csvDataMap, transactionEntity);
        assertThat(csvDataMap.get("Net"), is("0.00"));
    }

    @Test
    void toMapShouldUseNetAmountForNetForFailedTransactionWhenAvailable() {

        TransactionEntity transactionEntity = transactionFixture.withNetAmount(-5L).toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertPaymentDetails(csvDataMap, transactionEntity);
        assertThat(csvDataMap.get("Net"), is("-0.05"));
    }

    @Test
    void toMapShouldUseNetAmountForNetForSuccessfulTransactionWhenAvailable() {
        TransactionEntity transactionEntity = transactionFixture
                .withState(TransactionState.SUCCESS)
                .withAmount(950L)
                .withTotalAmount(1000L)
                .withNetAmount(1150L)
                .toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertPaymentDetails(csvDataMap, transactionEntity);
        assertThat(csvDataMap.get("Net"), is("11.50"));
    }

    @Test
    void toMapShouldUseTotalAmountForNetForSuccessfulTransactionWhenNetUnavailable() {

        TransactionEntity transactionEntity = transactionFixture
                .withState(TransactionState.SUCCESS)
                .withAmount(950L)
                .withTotalAmount(1000L)
                .toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertPaymentDetails(csvDataMap, transactionEntity);
        assertThat(csvDataMap.get("Net"), is("10.00"));
    }

    @Test
    void toMapShouldUseAmountForNetForSuccessfulTransactionWhenNetAndTotalUnavailable() {
        TransactionEntity transactionEntity = transactionFixture
                .withState(TransactionState.SUCCESS)
                .withAmount(950L)
                .toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertPaymentDetails(csvDataMap, transactionEntity);
        assertThat(csvDataMap.get("Net"), is("9.50"));
    }

    @Test
    void toMapShouldIncludeExternalMetadataFields() {
        TransactionEntity transactionEntity = transactionFixture.withTransactionDetails(
                new GsonBuilder().create().toJson(ImmutableMap.builder()
                        .put("external_metadata",
                                ImmutableMap.builder()
                                        .put("key-1", "value-1").put("key-2", "value-2").build())
                        .build()
                )).toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertThat(csvDataMap.get("key-1 (metadata)"), is("value-1"));
        assertThat(csvDataMap.get("key-2 (metadata)"), is("value-2"));
    }

    @Test
    void toMapShouldIncludeFeeAndNetAmountForStripePayments() {
        TransactionEntity transactionEntity = transactionFixture.withNetAmount(594)
                .withPaymentProvider("stripe")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withFee(6L).toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertThat(csvDataMap.get("Net"), is("5.94"));
        assertThat(csvDataMap.get("Fee"), is("0.06"));
    }

    @Test
    void toMapShouldIncludeFeeBreakdownForAvailableFeeType() {
        JSONObject feeRadar = new JSONObject()
                .put("fee_type", "radar")
                .put("amount", 3);

        JSONObject feeBreadownJsonObject = new JSONObject().put("fee_breakdown", new JSONArray()
                .put(feeRadar));

        TransactionEntity transactionEntity = transactionFixture.withNetAmount(594)
                .withPaymentProvider("stripe")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withTransactionDetails(feeBreadownJsonObject.toString())
                .withFee(6L).toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertThat(csvDataMap.get("Fee (fraud protection)"), is("0.03"));
        assertThat(csvDataMap.get("Fee (transaction)"), is(nullValue()));
        assertThat(csvDataMap.get("Fee (3DS)"), is(nullValue()));
    }

    @Test
    void getCsvHeadersWithMedataKeysShouldReturnMapWithCorrectCsvHeaders_WithoutOptionalColumns() {
        var keys = List.of("test-key-1", "test-key-2");

        Map<String, Object> csvHeaders = csvTransactionFactory.getCsvHeadersWithMedataKeys(keys, false, false);

        assertThat(csvHeaders.get("Reference"), is(notNullValue()));
        assertThat(csvHeaders.get("Description"), is(notNullValue()));
        assertThat(csvHeaders.get("Email"), is(notNullValue()));
        assertThat(csvHeaders.get("Amount"), is(notNullValue()));
        assertThat(csvHeaders.get("Card Brand"), is(notNullValue()));
        assertThat(csvHeaders.get("Cardholder Name"), is(notNullValue()));
        assertThat(csvHeaders.get("Card Expiry Date"), is(notNullValue()));
        assertThat(csvHeaders.get("Card Number"), is(notNullValue()));
        assertThat(csvHeaders.get("State"), is(notNullValue()));
        assertThat(csvHeaders.get("Finished"), is(notNullValue()));
        assertThat(csvHeaders.get("Error Code"), is(notNullValue()));
        assertThat(csvHeaders.get("Error Message"), is(notNullValue()));
        assertThat(csvHeaders.get("Provider ID"), is(notNullValue()));
        assertThat(csvHeaders.get("GOV.UK Payment ID"), is(notNullValue()));
        assertThat(csvHeaders.get("Issued By"), is(notNullValue()));
        assertThat(csvHeaders.get("Date Created"), is(notNullValue()));
        assertThat(csvHeaders.get("Time Created"), is(notNullValue()));
        assertThat(csvHeaders.get("Corporate Card Surcharge"), is(notNullValue()));
        assertThat(csvHeaders.get("Wallet Type"), is(notNullValue()));
        assertThat(csvHeaders.get("Card Type"), is(notNullValue()));
        assertThat(csvHeaders.get("Payment Provider"), is(notNullValue()));
        assertThat(csvHeaders.get("3-D Secure Required"), is(notNullValue()));

        assertThat(csvHeaders.get("test-key-1 (metadata)"), is(notNullValue()));
        assertThat(csvHeaders.get("test-key-2 (metadata)"), is(notNullValue()));

        assertThat(csvHeaders.get("Net"), is(nullValue()));
        assertThat(csvHeaders.get("Fee"), is(nullValue()));
        assertThat(csvHeaders.get("Fee (transaction)"), is(nullValue()));
        assertThat(csvHeaders.get("Fee (fraud protection)"), is(nullValue()));
        assertThat(csvHeaders.get("Fee (3DS)"), is(nullValue()));
        assertThat(csvHeaders.get("MOTO"), is(nullValue()));
    }

    @Test
    void getCsvHeadersWithMedataKeysShouldReturnMapWithCorrectCsvHeaders_WithFeeBreakdownColumns() {
        var keys = List.of("test-key-1", "test-key-2");

        Map<String, Object> csvHeaders = csvTransactionFactory.getCsvHeadersWithMedataKeys(keys, true, false);

        assertThat(csvHeaders.get("Net"), is(notNullValue()));
        assertThat(csvHeaders.get("Fee"), is(notNullValue()));
        assertThat(csvHeaders.get("Fee (transaction)"), is(notNullValue()));
        assertThat(csvHeaders.get("Fee (fraud protection)"), is(notNullValue()));
        assertThat(csvHeaders.get("Fee (3DS)"), is(notNullValue()));
    }

    @Test
    void getCsvHeadersWithMedataKeysShouldReturnMapWithCorrectCsvHeaders_WithMotoColumn() {
        var keys = List.of("test-key-1", "test-key-2");

        Map<String, Object> csvHeaders = csvTransactionFactory.getCsvHeadersWithMedataKeys(keys, false, true);

        assertThat(csvHeaders.get("MOTO"), is(notNullValue()));
    }

    @Test
    void shouldSanitiseValuesCorrectlyAgainstSpreadsheetFormulaInjection() {
        TransactionEntity transactionEntity = transactionFixture.withNetAmount(594)
                .withReference("=ref-1")
                .withDescription("+desc-1")
                .withEmail("@email.com")
                .withCardholderName("-J Doe")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withFee(6L).toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertThat(csvDataMap.get("Reference"), is("'=ref-1"));
        assertThat(csvDataMap.get("Description"), is("'+desc-1"));
        assertThat(csvDataMap.get("Email"), is("'@email.com"));
        assertThat(csvDataMap.get("Cardholder Name"), is("'-J Doe"));
    }

    @Test
    void toMapShouldIncludeShouldInclude3DSecureRequired() {
        var transactionEntity = transactionFixture
                .withTotalAmount(123L)
                .withVersion3ds("2.0.1")
                .withDefaultTransactionDetails()
                .toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertPaymentDetails(csvDataMap, transactionEntity);

        assertThat(csvDataMap.get("Amount"), is("1.00"));
        assertThat(csvDataMap.get("GOV.UK Payment ID"), is(transactionEntity.getExternalId()));
        assertThat(csvDataMap.get("Provider ID"), is(transactionEntity.getGatewayTransactionId()));
        assertThat(csvDataMap.get("State"), is("Declined"));
        assertThat(csvDataMap.get("Finished"), is(true));
        assertThat(csvDataMap.get("Error Code"), is("P0010"));
        assertThat(csvDataMap.get("Error Message"), is("Payment method rejected"));
        assertThat(csvDataMap.get("Date Created"), is("12 Mar 2018"));
        assertThat(csvDataMap.get("Time Created"), is("16:25:01"));
        assertThat(csvDataMap.get("Corporate Card Surcharge"), is("0.23"));
        assertThat(csvDataMap.get("Total Amount"), is("0.00"));
        assertThat(csvDataMap.get("MOTO"), is(true));
        assertThat(csvDataMap.get("Payment Provider"), is("sandbox"));
        assertThat(csvDataMap.get("3-D Secure Required"), is(true));
    }

    private void assertPaymentDetails(Map<String, Object> csvDataMap, TransactionEntity transactionEntity) {
        assertThat(csvDataMap.get("Reference"), is(transactionEntity.getReference()));
        assertThat(csvDataMap.get("Description"), is(transactionEntity.getDescription()));
        assertThat(csvDataMap.get("Email"), is(transactionEntity.getEmail()));
        assertThat(csvDataMap.get("Card Brand"), is("Visa"));
        assertThat(csvDataMap.get("Cardholder Name"), is(transactionEntity.getCardholderName()));
        assertThat(csvDataMap.get("Card Expiry Date"), is("10/21"));
        assertThat(csvDataMap.get("Card Number"), is(transactionEntity.getLastDigitsCardNumber()));
        assertThat(csvDataMap.get("Card Type"), is("credit"));
        assertThat(csvDataMap.get("Wallet Type"), is("Apple Pay"));
    }
}