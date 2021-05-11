package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
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

public class CsvTransactionFactoryTest {

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
                .withTotalAmount(123L)
                .withMoto(true)
                .withCorporateCardSurcharge(23)
                .withCardBrandLabel("Visa")
                .withDefaultCardDetails()
                .withDefaultTransactionDetails();
    }

    @Test
    public void toMapShouldReturnMapWithCorrectCsvDataForPaymentTransaction() {

        TransactionEntity transactionEntity = transactionFixture.toEntity();

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
        assertThat(csvDataMap.get("Total Amount"), is("1.23"));
        assertThat(csvDataMap.get("MOTO"), is(true));
    }

    @Test
    public void toMapShouldReturnMapWithCorrectCsvDataForRefundTransaction() {

        TransactionFixture refundTransactionFixture = aTransactionFixture()
                .withCreatedDate(ZonedDateTime.parse("2018-03-12T16:25:01.123456Z"))
                .withTransactionType("REFUND")
                .withAmount(99L)
                .withTotalAmount(99L)
                .withState(TransactionState.ERROR)
                .withGatewayTransactionId("refund-gateway-transaction-id")
                .withRefundedByUserEmail("refundedbyuser@example.org")
                .withParentExternalId("parent-external-id")
                .withDefaultPaymentDetails()
                .withDefaultTransactionDetails();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(refundTransactionFixture.toEntity());

        assertPaymentDetails(csvDataMap, refundTransactionFixture.toEntity());
        assertThat(csvDataMap.get("Amount"), is("-0.99"));
        assertThat(csvDataMap.get("Provider ID"), is(refundTransactionFixture.getGatewayTransactionId()));
        assertThat(csvDataMap.get("GOV.UK Payment ID"), is(refundTransactionFixture.getParentExternalId()));
        assertThat(csvDataMap.get("State"), is("Refund error"));
        assertThat(csvDataMap.get("Finished"), is(true));
        assertThat(csvDataMap.get("Error Code"), is("P0050"));
        assertThat(csvDataMap.get("Error Message"), is("Payment provider returned an error"));
        assertThat(csvDataMap.get("Date Created"), is("12 Mar 2018"));
        assertThat(csvDataMap.get("Time Created"), is("16:25:01"));
        assertThat(csvDataMap.get("Corporate Card Surcharge"), is("0.00"));
        assertThat(csvDataMap.get("Total Amount"), is("-0.99"));
        assertThat(csvDataMap.get("Net"), is("-0.99"));
        assertThat(csvDataMap.get("MOTO"), is(nullValue()));
    }

    @Test
    public void toMapShouldIncludeExternalMetadataFields() {
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
    public void toMapShouldIncludeFeeAndNetAmountForStripePayments() {
        TransactionEntity transactionEntity = transactionFixture.withNetAmount(594)
                .withPaymentProvider("stripe")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withFee(6L).toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertThat(csvDataMap.get("Net"), is("5.94"));
        assertThat(csvDataMap.get("Fee"), is("0.06"));
    }

    @Test
    public void getCsvHeadersWithMedataKeysShouldReturnMapWithCorrectCsvHeaders_WithoutOptionalColumns() {
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

        assertThat(csvHeaders.get("test-key-1 (metadata)"), is(notNullValue()));
        assertThat(csvHeaders.get("test-key-2 (metadata)"), is(notNullValue()));

        assertThat(csvHeaders.get("Net"), is(nullValue()));
        assertThat(csvHeaders.get("Fee"), is(nullValue()));
        assertThat(csvHeaders.get("MOTO"), is(nullValue()));
    }

    @Test
    public void getCsvHeadersWithMedataKeysShouldReturnMapWithCorrectCsvHeaders_WithFeeColumns() {
        var keys = List.of("test-key-1", "test-key-2");

        Map<String, Object> csvHeaders = csvTransactionFactory.getCsvHeadersWithMedataKeys(keys, true, false);

        assertThat(csvHeaders.get("Net"), is(notNullValue()));
        assertThat(csvHeaders.get("Fee"), is(notNullValue()));
    }

    @Test
    public void getCsvHeadersWithMedataKeysShouldReturnMapWithCorrectCsvHeaders_WithMotoColumn() {
        var keys = List.of("test-key-1", "test-key-2");

        Map<String, Object> csvHeaders = csvTransactionFactory.getCsvHeadersWithMedataKeys(keys, false, true);

        assertThat(csvHeaders.get("MOTO"), is(notNullValue()));
    }

    @Test
    public void shouldSanitiseValuesCorrectlyAgainstSpreadsheetFormulaInjection(){
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