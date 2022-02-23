package uk.gov.pay.ledger.transaction.resource;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.metadatakey.dao.MetadataKeyDao;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.transactionmetadata.dao.TransactionMetadataDao;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZonedDateTime.now;
import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.csv.CSVFormat.RFC4180;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.GatewayAccountMetadataFixture.aGatewayAccountMetadataFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class TransactionResourceCsvIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private Integer port = rule.getAppRule().getLocalPort();
    private MetadataKeyDao metadataKeyDao;
    private TransactionMetadataDao transactionMetadataDao;

    @BeforeEach
    public void setUp() {
        transactionMetadataDao = new TransactionMetadataDao(rule.getJdbi());
        metadataKeyDao = rule.getJdbi().onDemand(MetadataKeyDao.class);
        aDatabaseTestHelper(rule.getJdbi()).truncateAllData();
    }

    @Test
    public void shouldGetAllTransactionsAsCSVWithAcceptType() throws IOException {
        String targetGatewayAccountId = "123";
        String otherGatewayAccountId = "456";

        TransactionFixture transactionFixture = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.ERROR_GATEWAY)
                .withAmount(123L)
                .withTotalAmount(123L)
                .withCorporateCardSurcharge(5L)
                .withCreatedDate(ZonedDateTime.parse("2018-03-12T16:25:01.123456Z"))
                .withGatewayAccountId(targetGatewayAccountId)
                .withLastDigitsCardNumber("1234")
                .withCardBrandLabel("Diners Club")
                .withDefaultCardDetails().withCardholderName("J Doe")
                .withGatewayTransactionId("gateway-transaction-id")
                .withExternalMetadata(ImmutableMap.of("test-key-1", "value1"))
                .withVersion3ds("2.0.1")
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.SUBMITTED)
                .withGatewayAccountId(otherGatewayAccountId)
                .insert(rule.getJdbi());

        TransactionFixture refundTransactionFixture = aTransactionFixture()
                .withTransactionType("REFUND")
                .withParentExternalId(transactionFixture.getExternalId())
                .withGatewayAccountId(transactionFixture.getGatewayAccountId())
                .withRefundedByUserEmail("refund-by-user-email@example.org")
                .withCreatedDate(ZonedDateTime.parse("2018-03-12T16:24:01.123456Z"))
                .withAmount(100L)
                .withTotalAmount(100L)
                .withState(TransactionState.ERROR_GATEWAY)
                .withGatewayTransactionId("refund-gateway-transaction-id")
                .withDefaultPaymentDetails()
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi());

        metadataKeyDao.insertIfNotExist("test-key-1");

        aGatewayAccountMetadataFixture()
                .withGatewayAccountId(targetGatewayAccountId)
                .withMetadataKey("test-key-1")
                .insert(rule.getJdbi());

        transactionMetadataDao.upsert(transactionFixture.getId(), "test-key-1", "value1");

        InputStream csvResponseStream = given().port(port)
                .accept("text/csv")
                .get("/v1/transaction/?" +
                        "account_id=" + targetGatewayAccountId +
                        "&page=1" +
                        "&display_size=5"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType("text/csv")
                .extract().asInputStream();

        List<CSVRecord> csvRecords = CSVParser.parse(csvResponseStream, UTF_8, RFC4180.withFirstRecordAsHeader()).getRecords();

        assertThat(csvRecords.size(), is(2));

        CSVRecord paymentRecord = csvRecords.get(0);
        assertThat(paymentRecord.size(), is(24));
        assertCommonPaymentFields(paymentRecord, transactionFixture);
        assertThat(paymentRecord.get("Amount"), is("1.23"));
        assertThat(paymentRecord.get("GOV.UK Payment ID"), is(transactionFixture.getExternalId()));
        assertThat(paymentRecord.get("Provider ID"), is(transactionFixture.getGatewayTransactionId()));
        assertThat(paymentRecord.get("State"), is("Error"));
        assertThat(paymentRecord.get("Finished"), is("true"));
        assertThat(paymentRecord.get("Error Code"), is("P0050"));
        assertThat(paymentRecord.get("Error Message"), is("Payment provider returned an error"));
        assertThat(paymentRecord.get("Date Created"), is("12 Mar 2018"));
        assertThat(paymentRecord.get("Time Created"), is("16:25:01"));
        assertThat(paymentRecord.get("Corporate Card Surcharge"), is("0.05"));
        assertThat(paymentRecord.get("Total Amount"), is("1.23"));
        assertThat(paymentRecord.get("test-key-1 (metadata)"), is("value1"));
        assertThat(paymentRecord.get("Wallet Type"), is("Apple Pay"));
        assertThat(paymentRecord.isMapped("Net"), is(false));
        assertThat(paymentRecord.isMapped("Fee"), is(false));
        assertThat(paymentRecord.isMapped("MOTO"), is(false));
        assertThat(paymentRecord.get("Payment Provider"), is("sandbox"));
        assertThat(paymentRecord.get("3-D Secure Required"), is("true"));

        CSVRecord refundRecord = csvRecords.get(1);
        assertCommonPaymentFields(refundRecord, refundTransactionFixture);
        assertThat(refundRecord.get("Amount"), is("-1.00"));
        assertThat(refundRecord.get("GOV.UK Payment ID"), is(refundTransactionFixture.getParentExternalId()));
        assertThat(refundRecord.get("Provider ID"), is(refundTransactionFixture.getGatewayTransactionId()));
        assertThat(refundRecord.get("State"), is("Refund error"));
        assertThat(refundRecord.get("Finished"), is("true"));
        assertThat(refundRecord.get("Error Code"), is("P0050"));
        assertThat(refundRecord.get("Error Message"), is("Payment provider returned an error"));
        assertThat(refundRecord.get("Date Created"), is("12 Mar 2018"));
        assertThat(refundRecord.get("Time Created"), is("16:24:01"));
        assertThat(refundRecord.get("Corporate Card Surcharge"), is("0.00"));
        assertThat(refundRecord.get("Total Amount"), is("-1.00"));
        assertThat(refundRecord.get("Wallet Type"), is("Apple Pay"));
        assertThat(refundRecord.get("Issued By"), is("refund-by-user-email@example.org"));
        assertThat(refundRecord.isMapped("MOTO"), is(false));
        assertThat(refundRecord.get("3-D Secure Required"), is(""));
    }

    @Test
    public void shouldGetAllTransactionsAsCSVWithAcceptTypeWithFeeHeaders() throws IOException {
        String gatewayAccountId = "123";

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType("PAYMENT")
                .withFee(100L)
                .withNetAmount(1100)
                .insert(rule.getJdbi());

        InputStream csvResponseStream = given().port(port)
                .accept("text/csv")
                .get("/v1/transaction/?" +
                        "account_id=" + gatewayAccountId +
                        "&fee_headers=true" +
                        "&page=1" +
                        "&display_size=5"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType("text/csv")
                .extract().asInputStream();

        List<CSVRecord> csvRecords = CSVParser.parse(csvResponseStream, UTF_8, RFC4180.withFirstRecordAsHeader()).getRecords();

        assertThat(csvRecords.size(), is(1));

        CSVRecord paymentRecord = csvRecords.get(0);
        assertThat(paymentRecord.size(), is(25));
        assertThat(paymentRecord.get("Net"), is("11.00"));
        assertThat(paymentRecord.get("Fee"), is("1.00"));
    }

    @Test
    public void shouldIncludeFeeBreakdownHeaders() throws IOException {
        String gatewayAccountId = "123";
        JSONObject feeTransaction = new JSONObject()
                .put("fee_type", "transaction")
                .put("amount", 10);
        JSONObject feeRadar = new JSONObject()
                .put("fee_type", "radar")
                .put("amount", 3);
        JSONObject fee3ds = new JSONObject()
                .put("fee_type", "three_ds")
                .put("amount", 5);

        JSONObject feeBreadownJsonObject = new JSONObject().put("fee_breakdown", new JSONArray()
                .put(feeTransaction)
                .put(feeRadar)
                .put(fee3ds));

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType("PAYMENT")
                .withFee(100L)
                .withNetAmount(1100)
                .withTransactionDetails(feeBreadownJsonObject.toString())
                .insert(rule.getJdbi());

        InputStream csvResponseStream = given().port(port)
                .accept("text/csv")
                .get("/v1/transaction/?" +
                        "account_id=" + gatewayAccountId +
                        "&fee_breakdown_headers=true" +
                        "&page=1" +
                        "&display_size=5"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType("text/csv")
                .extract().asInputStream();


        List<CSVRecord> csvRecords = CSVParser.parse(csvResponseStream, UTF_8, RFC4180.withFirstRecordAsHeader()).getRecords();

        assertThat(csvRecords.size(), is(1));

        CSVRecord paymentRecord = csvRecords.get(0);
        assertThat(paymentRecord.get("Fee (transaction)"), is("0.10"));
        assertThat(paymentRecord.get("Fee (fraud protection)"), is("0.03"));
        assertThat(paymentRecord.get("Fee (3DS)"), is("0.05"));
    }

    @Test
    public void shouldGetAllTransactionsAsCSVWithAcceptTypeWithMotoHeader() throws IOException {
        String gatewayAccountId = "123";

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType("PAYMENT")
                .withMoto(true)
                .insert(rule.getJdbi());

        InputStream csvResponseStream = given().port(port)
                .accept("text/csv")
                .get("/v1/transaction/?" +
                        "account_id=" + gatewayAccountId +
                        "&moto_header=true" +
                        "&page=1" +
                        "&display_size=5"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType("text/csv")
                .extract().asInputStream();

        List<CSVRecord> csvRecords = CSVParser.parse(csvResponseStream, UTF_8, RFC4180.withFirstRecordAsHeader()).getRecords();

        assertThat(csvRecords.size(), is(1));

        CSVRecord paymentRecord = csvRecords.get(0);
        assertThat(paymentRecord.size(), is(24));
        assertThat(paymentRecord.get("MOTO"), is("true"));
    }

    @Test
    public void shouldReturnMultipleAccountTransactions_whenRequestedAsCsv() throws IOException {
        String gatewayAccountId = "123";
        String gatewayAccountId2 = "456";

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType("PAYMENT")
                .withFee(100L)
                .withNetAmount(1100)
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId2)
                .withTransactionType("PAYMENT")
                .withFee(200L)
                .withNetAmount(1000)
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withGatewayAccountId("789")
                .withTransactionType("PAYMENT")
                .withFee(100L)
                .withNetAmount(1100)
                .insert(rule.getJdbi());

        InputStream csvResponseStream = given().port(port)
                .accept("text/csv")
                .queryParam("account_id", gatewayAccountId + "," + gatewayAccountId2)
                .queryParam("fee_headers", true)
                .queryParam("page", 1)
                .queryParam("display_size", 5)
                .get("/v1/transaction/")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType("text/csv")
                .extract().asInputStream();

        List<CSVRecord> csvRecords = CSVParser.parse(csvResponseStream, UTF_8, RFC4180.withFirstRecordAsHeader()).getRecords();

        assertThat(csvRecords.size(), is(2));

        CSVRecord paymentRecord = csvRecords.get(0);
        assertThat(paymentRecord.size(), is(25));
        assertThat(paymentRecord.get("Net"), is("10.00"));
        assertThat(paymentRecord.get("Fee"), is("2.00"));
        CSVRecord paymentRecord2 = csvRecords.get(1);
        assertThat(paymentRecord2.size(), is(25));
        assertThat(paymentRecord2.get("Net"), is("11.00"));
        assertThat(paymentRecord2.get("Fee"), is("1.00"));
    }

    @Test
    public void shouldReturnCSVHeadersInCorrectOrder() throws IOException {
        String targetGatewayAccountId = "123";

        String metadataKey = "a-metadata-key";
        TransactionFixture transactionFixture = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withGatewayAccountId(targetGatewayAccountId)
                .withExternalMetadata(ImmutableMap.of(metadataKey, "value1"))
                .withFee(100L)
                .withNetAmount(1100)
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi());

        metadataKeyDao.insertIfNotExist(metadataKey);
        transactionMetadataDao.upsert(transactionFixture.getId(), metadataKey, "value1");

        aGatewayAccountMetadataFixture()
                .withGatewayAccountId(targetGatewayAccountId)
                .withMetadataKey("a-metadata-key")
                .insert(rule.getJdbi());

        InputStream csvResponseStream = given().port(port)
                .accept("text/csv")
                .get("/v1/transaction?" +
                        "account_id=" + targetGatewayAccountId +
                        "&fee_headers=true" +
                        "&moto_header=true" +
                        "&page=1" +
                        "&display_size=5"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType("text/csv")
                .extract().asInputStream();

        List<CSVRecord> csvRecords = CSVParser.parse(csvResponseStream, UTF_8, DEFAULT).getRecords();

        CSVRecord header = csvRecords.get(0);
        assertThat(header.size(), is(27));
        assertThat(header.get(0), is("Reference"));
        assertThat(header.get(1), is("Description"));
        assertThat(header.get(2), is("Email"));
        assertThat(header.get(3), is("Amount"));
        assertThat(header.get(4), is("Card Brand"));
        assertThat(header.get(5), is("Cardholder Name"));
        assertThat(header.get(6), is("Card Expiry Date"));
        assertThat(header.get(7), is("Card Number"));
        assertThat(header.get(8), is("State"));
        assertThat(header.get(9), is("Finished"));
        assertThat(header.get(10), is("Error Code"));
        assertThat(header.get(11), is("Error Message"));
        assertThat(header.get(12), is("Provider ID"));
        assertThat(header.get(13), is("GOV.UK Payment ID"));
        assertThat(header.get(14), is("Issued By"));
        assertThat(header.get(15), is("Date Created"));
        assertThat(header.get(16), is("Time Created"));
        assertThat(header.get(17), is("Corporate Card Surcharge"));
        assertThat(header.get(18), is("Total Amount"));
        assertThat(header.get(19), is("Wallet Type"));
        assertThat(header.get(20), is("Fee"));
        assertThat(header.get(21), is("Net"));
        assertThat(header.get(22), is("Card Type"));
        assertThat(header.get(23), is("MOTO"));
        assertThat(header.get(24), is("Payment Provider"));
        assertThat(header.get(25), is("3-D Secure Required"));
        assertThat(header.get(26), is("a-metadata-key (metadata)"));
    }

    @Test
    public void shouldGetAllTransactionsForAGivenGatewayPayoutId() throws IOException {
        String targetGatewayAccountId = randomNumeric(5);
        String gatewayPayoutId = randomAlphanumeric(30);

        TransactionFixture transactionFixture = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withGatewayAccountId(targetGatewayAccountId)
                .withCreatedDate(now())
                .withGatewayPayoutId(gatewayPayoutId)
                .insert(rule.getJdbi());
        TransactionFixture transactionFixtureWithoutGatewayPayoutId = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withGatewayAccountId(targetGatewayAccountId)
                .insert(rule.getJdbi());
        TransactionFixture refundFixture = aTransactionFixture()
                .withTransactionType("REFUND")
                .withCreatedDate(now().plusDays(1))
                .withParentExternalId(transactionFixtureWithoutGatewayPayoutId.getExternalId())
                .withGatewayAccountId(targetGatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId)
                .insert(rule.getJdbi());

        InputStream csvResponseStream = given().port(port)
                .accept("text/csv")
                .get("/v1/transaction?" +
                        "account_id=" + targetGatewayAccountId +
                        "&gateway_payout_id=" + gatewayPayoutId
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType("text/csv")
                .extract().asInputStream();

        List<CSVRecord> csvRecords = CSVParser.parse(csvResponseStream, UTF_8, RFC4180.withFirstRecordAsHeader()).getRecords();
        assertThat(csvRecords.size(), is(2));

        CSVRecord refundRecord = csvRecords.get(0);
        assertThat(refundRecord.get("GOV.UK Payment ID"), is(transactionFixtureWithoutGatewayPayoutId.getExternalId()));
        CSVRecord paymentRecord = csvRecords.get(1);
        assertThat(paymentRecord.get("GOV.UK Payment ID"), is(transactionFixture.getExternalId()));
    }

    @Test
    public void shouldGetTransactionsWithCorrectMetadataColumnsForSearchByCSV() throws IOException {
        String targetGatewayAccountId = "123";

        TransactionFixture transactionFixture = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withReference("ref1")
                .withGatewayAccountId(targetGatewayAccountId)
                .withExternalMetadata(ImmutableMap.of("a-metadata-key", "value1", "metadata-key-2", "value2"))
                .withDefaultTransactionDetails()
                .insertTransactionAndTransactionMetadata(rule.getJdbi());
        TransactionFixture transactionWithoutMetadataToExclude = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withReference("ref1")
                .withGatewayAccountId(targetGatewayAccountId)
                .withDefaultTransactionDetails()
                .insertTransactionAndTransactionMetadata(rule.getJdbi());

        aGatewayAccountMetadataFixture()
                .withGatewayAccountId(targetGatewayAccountId)
                .withMetadataKey("a-metadata-key")
                .insert(rule.getJdbi());
        aGatewayAccountMetadataFixture()
                .withGatewayAccountId(targetGatewayAccountId)
                .withMetadataKey("metadata-key-2")
                .insert(rule.getJdbi());

        InputStream csvResponseStream = given().port(port)
                .accept("text/csv")
                .get("/v1/transaction?" +
                        "account_id=" + targetGatewayAccountId +
                        "&metadata_value=value1")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType("text/csv")
                .extract().asInputStream();

        List<CSVRecord> csvRecords = CSVParser.parse(csvResponseStream, UTF_8, RFC4180.withFirstRecordAsHeader()).getRecords();

        assertThat(csvRecords.size(), is(1));
        CSVRecord record = csvRecords.get(0);
        assertThat(record.get("a-metadata-key (metadata)"), is("value1"));
        assertThat(record.get("metadata-key-2 (metadata)"), is("value2"));
    }

    private void assertCommonPaymentFields(CSVRecord csvRecord, TransactionFixture transactionFixture) {
        assertThat(csvRecord.get("Reference"), is(transactionFixture.getReference()));
        assertThat(csvRecord.get("Description"), is(transactionFixture.getDescription()));
        assertThat(csvRecord.get("Email"), is(transactionFixture.getEmail()));
        assertThat(csvRecord.get("Card Brand"), is(transactionFixture.getCardBrandLabel()));
        assertThat(csvRecord.get("Cardholder Name"), is(transactionFixture.getCardholderName()));
        assertThat(csvRecord.get("Card Expiry Date"), is(transactionFixture.getCardExpiryDate()));
        assertThat(csvRecord.get("Card Number"), is("1234"));
        assertThat(csvRecord.get("Card Type"), is("credit"));
    }
}