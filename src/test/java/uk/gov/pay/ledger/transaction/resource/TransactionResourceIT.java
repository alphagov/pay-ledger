package uk.gov.pay.ledger.transaction.resource;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.commons.model.Source;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.metadatakey.dao.MetadataKeyDao;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.Payment;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.transactionmetadata.dao.TransactionMetadataDao;
import uk.gov.pay.ledger.util.fixture.EventFixture;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.time.ZoneOffset.UTC;
import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.csv.CSVFormat.RFC4180;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.pay.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aPersistedTransactionList;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class TransactionResourceIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private Integer port = rule.getAppRule().getLocalPort();

    private TransactionFixture transactionFixture;
    private EventFixture eventFixture;
    private MetadataKeyDao metadataKeyDao;
    private TransactionMetadataDao transactionMetadataDao;

    @Before
    public void setUp() {
        transactionMetadataDao = new TransactionMetadataDao(rule.getJdbi());
        metadataKeyDao = rule.getJdbi().onDemand(MetadataKeyDao.class);
        aDatabaseTestHelper(rule.getJdbi()).truncateAllData();
    }

    @Test
    public void shouldGetTransaction() {

        transactionFixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withDefaultTransactionDetails()
                .withLive(true)
                .withSource(String.valueOf(Source.CARD_PAYMENT_LINK));
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("live", is(true))
                .body("source", is("CARD_PAYMENT_LINK"))
                .body("transaction_id", is(transactionFixture.getExternalId()))
                .body("card_details.cardholder_name", is(transactionFixture.getCardDetails().getCardHolderName()))
                .body("card_details.expiry_date", is(transactionFixture.getCardDetails().getExpiryDate()))
                .body("card_details.card_type", is(transactionFixture.getCardDetails().getCardType().toString().toLowerCase()))
                .body("card_details.billing_address.line1", is(transactionFixture.getCardDetails().getBillingAddress().getAddressLine1()));
    }

    @Test
    public void shouldReturn404ForNonExistentTransaction() {
        transactionFixture = aTransactionFixture();
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/does-not-exist?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void shouldReturnTransactionSearchResponseObjectForNonExistentTransactionByReference() {
        transactionFixture = aTransactionFixture()
                .withReference("existing");
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction" +
                        "?reference=not-existing" +
                        "&account_id=" + transactionFixture.getGatewayAccountId()
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("page", is(1))
                .body("total", is(0))
                .body("count", is(0))
                .body("results.size()", is(0));
    }

    @Test
    public void shouldGetAllTransactionsForAmbiguousExternalState() {

        TransactionFixture cancelledTransaction1 = aTransactionFixture()
                .withDefaultCardDetails()
                .withGatewayAccountId("123")
                .withDefaultTransactionDetails()
                .withState(TransactionState.FAILED_CANCELLED)
                .insert(rule.getJdbi());
        TransactionFixture cancelledTransaction2 = aTransactionFixture()
                .withGatewayAccountId("123")
                .withDefaultCardDetails()
                .withDefaultTransactionDetails()
                .withState(TransactionState.CANCELLED)
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction" +
                        "?account_id=123" +
                        "&state=cancelled"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("page", is(1))
                .body("results[0].transaction_id", is(cancelledTransaction2.getExternalId()))
                .body("results[1].transaction_id", is(cancelledTransaction1.getExternalId()));
    }

    @Test
    public void shouldGetRefundTransaction() {
        var now = ZonedDateTime.parse("2019-07-31T14:52:07.073Z");
        var refundedBy = "some_user_id";
        var refundedByUserEmail = "test@example.com";

        transactionFixture = aTransactionFixture()
                .withTransactionType("REFUND")
                .withState(TransactionState.SUCCESS)
                .withCreatedDate(now)
                .withRefundedById(refundedBy)
                .withRefundedByUserEmail(refundedByUserEmail)
                .withDefaultTransactionDetails();
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "?account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("transaction_id", is(transactionFixture.getExternalId()))
                .body("state.status", is(transactionFixture.getState().getStatus()))
                .body("amount", is(transactionFixture.getAmount().intValue()))
                .body("reference", is(transactionFixture.getReference()))
                .body("created_date", is(now.toString()))
                .body("refunded_by", is(refundedBy))
                .body("refunded_by_user_email", is(refundedByUserEmail));
    }

    @Test
    public void shouldSearchUsingAllFieldsAndReturnAllFieldsCorrectly() {
        String gatewayAccountId = RandomStringUtils.randomAlphanumeric(20);
        List<Transaction> transactionList = aPersistedTransactionList(gatewayAccountId, 20, rule.getJdbi(), true);
        Payment transactionToVerify = (Payment) transactionList.get(15);
        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                //todo: add more query params (refund_states, payment_states...) when search functionality is available
                .get("/v1/transaction?" +
                        "account_id=" + gatewayAccountId +
                        "&page=2" +
                        "&display_size=2" +
                        "&email=example.org" +
                        "&reference=reference" +
                        "&cardholder_name=smith" +
                        "&from_date=2000-01-01T10:15:30Z" +
                        "&to_date=2100-01-01T10:15:30Z" +
                        "&state=submitted" +
                        "&card_brands=visa,mastercard" +
                        "&last_digits_card_number=1234" +
                        "&first_digits_card_number=123456" +
                        "&transaction_type=payment"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("results[0].gateway_account_id", is(transactionToVerify.getGatewayAccountId()))
                .body("results[0].amount", is(transactionToVerify.getAmount().intValue()))
                .body("results[0].state.finished", is(false))
                .body("results[0].state.status", is(transactionToVerify.getState().getStatus()))
                .body("results[0].description", is(transactionToVerify.getDescription()))
                .body("results[0].reference", is(transactionToVerify.getReference()))
                .body("results[0].moto", is(transactionToVerify.getMoto()))
                .body("results[0].language", is(transactionToVerify.getLanguage()))
                .body("results[0].return_url", is(transactionToVerify.getReturnUrl()))
                .body("results[0].email", is(transactionToVerify.getEmail()))
                .body("results[0].created_date", is(ISO_INSTANT_MILLISECOND_PRECISION.format(transactionToVerify.getCreatedDate())))
                .body("results[0].payment_provider", is(transactionToVerify.getPaymentProvider()))
                .body("results[0].card_details.cardholder_name", is(transactionToVerify.getCardDetails().getCardHolderName()))
                .body("results[0].card_details.billing_address.line1", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressLine1()))
                .body("results[0].card_details.billing_address.line2", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressLine2()))
                .body("results[0].card_details.billing_address.postcode", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressPostCode()))
                .body("results[0].card_details.billing_address.city", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressCity()))
                .body("results[0].card_details.billing_address.country", is(transactionToVerify.getCardDetails().getBillingAddress().getAddressCountry()))
                .body("results[0].card_details.card_brand", is(transactionToVerify.getCardDetails().getCardBrand()))
                .body("results[0].card_details.expiry_date", is(transactionToVerify.getCardDetails().getExpiryDate()))
                .body("results[0].delayed_capture", is(transactionToVerify.getDelayedCapture()))
                .body("results[0].transaction_id", is(transactionToVerify.getExternalId()))
                .body("results[0].transaction_type", is(TransactionType.PAYMENT.name()))
                .body("results[0].net_amount", is(nullValue()))
                .body("results[0].total_amount", is(nullValue()))
                .body("results[0].fee", is(nullValue()))
                .body("results[0].live", is(true))
                .body("results[0].source", is("CARD_API"))

                .body("results[0].refund_summary.amount_available", is(transactionToVerify.getRefundSummary().getAmountAvailable().intValue()))
                .body("results[0].refund_summary.amount_submitted", is(transactionToVerify.getRefundSummary().getAmountSubmitted().intValue()))
                .body("results[0].refund_summary.amount_refunded", is(transactionToVerify.getRefundSummary().getAmountRefunded().intValue()))

                .body("count", is(2))
                .body("page", is(2))
                .body("total", is(10))

                .body("_links.self.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&cardholder_name=smith&first_digits_card_number=123456&last_digits_card_number=1234&card_brand=visa%2Cmastercard&state=submitted&transaction_type=PAYMENT&page=2&display_size=2"))
                .body("_links.first_page.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&cardholder_name=smith&first_digits_card_number=123456&last_digits_card_number=1234&card_brand=visa%2Cmastercard&state=submitted&transaction_type=PAYMENT&page=1&display_size=2"))
                .body("_links.last_page.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&cardholder_name=smith&first_digits_card_number=123456&last_digits_card_number=1234&card_brand=visa%2Cmastercard&state=submitted&transaction_type=PAYMENT&page=5&display_size=2"))
                .body("_links.prev_page.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&cardholder_name=smith&first_digits_card_number=123456&last_digits_card_number=1234&card_brand=visa%2Cmastercard&state=submitted&transaction_type=PAYMENT&page=1&display_size=2"))
                .body("_links.next_page.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_date=2000-01-01T10%3A15%3A30Z&to_date=2100-01-01T10%3A15%3A30Z&email=example.org&reference=reference&cardholder_name=smith&first_digits_card_number=123456&last_digits_card_number=1234&card_brand=visa%2Cmastercard&state=submitted&transaction_type=PAYMENT&page=3&display_size=2"));
    }

    @Test
    public void shouldSearchCorrectlyUsingPaymentStates() {
        String gatewayAccountId = "1";
        TransactionFixture createdTransaction = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.CREATED)
                .withDefaultCardDetails()
                .withGatewayAccountId(gatewayAccountId)
                .withDefaultTransactionDetails()
                .withCreatedDate(ZonedDateTime.now())
                .insert(rule.getJdbi());


        TransactionFixture submittedTransaction = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.SUBMITTED)
                .withDefaultCardDetails()
                .withGatewayAccountId(gatewayAccountId)
                .withDefaultTransactionDetails()
                .withCreatedDate(ZonedDateTime.now().minusHours(1))
                .insert(rule.getJdbi());

        TransactionFixture successTransaction = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.SUCCESS)
                .withDefaultCardDetails()
                .withGatewayAccountId(gatewayAccountId)
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi());


        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?" +
                        "account_id=" + gatewayAccountId +
                        "&page=1" +
                        "&display_size=5" +
                        "&payment_states=created,submitted"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(2))
                .body("results[0].transaction_id", is(createdTransaction.getExternalId()))
                .body("results[1].transaction_id", is(submittedTransaction.getExternalId()));
    }

    @Test
    public void shouldSearchCorrectlyUsingRefundStates() {
        String gatewayAccountId = "2";
        TransactionFixture createdTransaction = aTransactionFixture()
                .withTransactionType("REFUND")
                .withState(TransactionState.SUCCESS)
                .withDefaultCardDetails()
                .withGatewayAccountId(gatewayAccountId)
                .withDefaultTransactionDetails()
                .withCreatedDate(ZonedDateTime.now())
                .insert(rule.getJdbi());


        TransactionFixture submittedTransaction = aTransactionFixture()
                .withTransactionType("REFUND")
                .withState(TransactionState.SUBMITTED)
                .withDefaultCardDetails()
                .withGatewayAccountId(gatewayAccountId)
                .withDefaultTransactionDetails()
                .withCreatedDate(ZonedDateTime.now().minusHours(1))
                .insert(rule.getJdbi());

        TransactionFixture successTransaction = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.SUBMITTED)
                .withDefaultCardDetails()
                .withGatewayAccountId(gatewayAccountId)
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?" +
                        "account_id=" + gatewayAccountId +
                        "&page=1" +
                        "&display_size=5" +
                        "&refund_states=success,submitted"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(2))
                .body("results[0].transaction_id", is(createdTransaction.getExternalId()))
                .body("results[1].transaction_id", is(submittedTransaction.getExternalId()));
    }

    @Test
    public void shouldSearchCorrectlyUsingBothPaymentAndRefundStates() {
        String gatewayAccountId = "3";
        TransactionFixture successfulRefund = aTransactionFixture()
                .withTransactionType("REFUND")
                .withState(TransactionState.SUCCESS)
                .withDefaultCardDetails()
                .withGatewayAccountId(gatewayAccountId)
                .withDefaultTransactionDetails()
                .withCreatedDate(ZonedDateTime.now())
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withTransactionType("REFUND")
                .withState(TransactionState.SUBMITTED)
                .withDefaultCardDetails()
                .withGatewayAccountId(gatewayAccountId)
                .withDefaultTransactionDetails()
                .withCreatedDate(ZonedDateTime.now().minusHours(1))
                .insert(rule.getJdbi());

        TransactionFixture submittedPayment = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.SUBMITTED)
                .withDefaultCardDetails()
                .withGatewayAccountId(gatewayAccountId)
                .withCreatedDate(ZonedDateTime.now().minusHours(2))
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?" +
                        "account_id=" + gatewayAccountId +
                        "&page=1" +
                        "&display_size=5" +
                        "&refund_states=success" +
                        "&payment_states=submitted"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(2))
                .body("results[0].transaction_id", is(successfulRefund.getExternalId()))
                .body("results[1].transaction_id", is(submittedPayment.getExternalId()));
    }

    @Test
    public void shouldSearchTransactionCorrectlyByStateAndStatusVersion1() {
        TransactionFixture submittedPayment = aTransactionFixture()
                .withState(TransactionState.FAILED_REJECTED)
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?" +
                        "account_id=" + submittedPayment.getGatewayAccountId() +
                        "&state=failed" +
                        "&status_version=1"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(1))
                .body("results[0].transaction_id", is(submittedPayment.getExternalId()))
                .body("results[0].state.status", is("failed"));
    }

    @Test
    public void shouldSearchTransactionCorrectlyByStateWithDefaultStatusVersion2() {
        TransactionFixture submittedPayment = aTransactionFixture()
                .withState(TransactionState.FAILED_REJECTED)
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?" +
                        "account_id=" + submittedPayment.getGatewayAccountId() +
                        "&state=declined"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(1))
                .body("results[0].transaction_id", is(submittedPayment.getExternalId()))
                .body("results[0].state.status", is("declined"));
    }

    @Test
    public void shouldSearchUsingGatewayAccountId() {
        String targetGatewayAccountId = "123";
        String otherGatewayAccountId = "456";


        TransactionFixture targetPayment = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.SUBMITTED)
                .withGatewayAccountId(targetGatewayAccountId)
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.SUBMITTED)
                .withGatewayAccountId(otherGatewayAccountId)
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?" +
                        "account_id=" + targetGatewayAccountId +
                        "&page=1" +
                        "&display_size=5"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(1))
                .body("results[0].transaction_id", is(targetPayment.getExternalId()));
    }

    @Test
    public void shouldAllowNotSupplyingGatewayAccountId() {
        String targetGatewayAccountId = "123";
        String otherGatewayAccountId = "456";


        TransactionFixture firstPayment = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.SUBMITTED)
                .withGatewayAccountId(targetGatewayAccountId)
                .insert(rule.getJdbi());

        TransactionFixture secondPayment = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.SUBMITTED)
                .withGatewayAccountId(otherGatewayAccountId)
                .withCreatedDate(ZonedDateTime.now().minusHours(1))
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?" +
                        "override_account_id_restriction=true"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(2))
                .body("results[0].transaction_id", is(firstPayment.getExternalId()))
                .body("results[1].transaction_id", is(secondPayment.getExternalId()));
    }

    @Test
    public void shouldReturnTransactionEventsCorrectly() {
        transactionFixture = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withDefaultCardDetails()
                .withDefaultTransactionDetails();
        transactionFixture.insert(rule.getJdbi());

        Event event = EventFixture.anEventFixture()
                .withResourceExternalId(transactionFixture.getExternalId())
                .withEventDate(ZonedDateTime.parse("2019-07-31T09:52:43.451Z"))
                .insert(rule.getJdbi())
                .toEntity();

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "/event?gateway_account_id=" + transactionFixture.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("transaction_id", is(transactionFixture.getExternalId()))
                .body("events[0].amount", is(transactionFixture.getAmount().intValue()))
                .body("events[0].state.status", is("created"))
                .body("events[0].state.finished", is(false))
                .body("events[0].resource_type", is("PAYMENT"))
                .body("events[0].event_type", is(event.getEventType()))
                .body("events[0].timestamp", is("2019-07-31T09:52:43.451Z"))
                .body("events[0].data.event_data", is("event data"));
    }

    @Test
    public void shouldReturnBadRequestStatusIfNoTransactionEventsFound() {
        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/some-external-id/event?gateway_account_id=some-gateway-id")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode())
                .contentType(JSON)
                .body("message", is("Transaction with id [some-external-id] not found"));
    }

    @Test
    public void shouldSearchCorrectlyOnTransactionAndParentTransaction_WhenWithParentTransactionIsTrue() {
        String gatewayAccountId = String.valueOf(nextLong());
        TransactionFixture payment = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.SUCCESS)
                .withLastDigitsCardNumber("4242")
                .withCardBrand("visa")
                .withCardBrandLabel("Visa")
                .withGatewayAccountId(gatewayAccountId)
                .withDefaultTransactionDetails()
                .withCreatedDate(ZonedDateTime.now(UTC).minusHours(1))
                .insert(rule.getJdbi());

        TransactionFixture successfulRefund = aTransactionFixture()
                .withTransactionType("REFUND")
                .withState(TransactionState.SUCCESS)
                .withParentExternalId(payment.getExternalId())
                .withGatewayAccountId(gatewayAccountId)
                .withCreatedDate(ZonedDateTime.now(UTC))
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?" +
                        "account_id=" + gatewayAccountId +
                        "&with_parent_transaction=true" +
                        "&card_brands=" + payment.getCardBrand() +
                        "&cardholder_name=" + payment.getCardholderName() +
                        "&email=" + payment.getEmail() +
                        "&reference=" + payment.getReference() +
                        "&last_digits_card_number=" + payment.getLastDigitsCardNumber()
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(2))
                .body("results[0].transaction_id", is(successfulRefund.getExternalId()))
                .body("results[0].transaction_type", is("REFUND"))
                .body("results[0].parent_transaction.transaction_id", is(payment.getExternalId()))
                .body("results[0].parent_transaction.transaction_type", is("PAYMENT"))
                .body("results[0].parent_transaction.reference", is(payment.getReference()))
                .body("results[0].parent_transaction.email", is(payment.getEmail()))
                .body("results[0].parent_transaction.card_details.card_brand", is("Visa"))
                .body("results[0].parent_transaction.card_details.last_digits_card_number", is(payment.getLastDigitsCardNumber()))
                .body("results[0].parent_transaction.card_details.cardholder_name", is(payment.getCardholderName()))
                .body("results[1].transaction_id", is(payment.getExternalId()))
                .body("results[1].parent_transaction", is(nullValue()));
    }

    @Test
    public void getTransactionsForTransactionShouldReturnTransactionsWithParentExternalId() {
        TransactionEntity parentTransactionEntity = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withDefaultCardDetails()
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity refundTransactionEntity = aTransactionFixture()
                .withParentExternalId(parentTransactionEntity.getExternalId())
                .withGatewayAccountId(parentTransactionEntity.getGatewayAccountId())
                .withTransactionType("REFUND")
                .withState(TransactionState.SUCCESS)
                .withAmount(1000L)
                .withRefundedById("refund-by-user-id")
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi())
                .toEntity();

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + parentTransactionEntity.getExternalId() + "/transaction?gateway_account_id=" + parentTransactionEntity.getGatewayAccountId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("parent_transaction_id", is(parentTransactionEntity.getExternalId()))
                .body("transactions[0].gateway_account_id", is(refundTransactionEntity.getGatewayAccountId()))
                .body("transactions[0].amount", is(1000))
                .body("transactions[0].state.status", is(refundTransactionEntity.getState().getStatus()))
                .body("transactions[0].state.finished", is(true))
                .body("transactions[0].reference", is(refundTransactionEntity.getReference()))
                .body("transactions[0].created_date", is(ISO_INSTANT_MILLISECOND_PRECISION.format(refundTransactionEntity.getCreatedDate())))
                .body("transactions[0].refunded_by", is("refund-by-user-id"))
                .body("transactions[0].transaction_type", is(refundTransactionEntity.getTransactionType()))
                .body("transactions[0].transaction_id", is(refundTransactionEntity.getExternalId()));
    }

    @Test
    public void getTransactionsForTransactionShouldReturnEmptyListIfParentTransactionHasNoTransactions() {
        transactionFixture = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withGatewayAccountId("1")
                .withDefaultCardDetails()
                .withDefaultTransactionDetails();
        transactionFixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/" + transactionFixture.getExternalId() + "/transaction?gateway_account_id=1")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("parent_transaction_id", is(transactionFixture.getExternalId()))
                .body("transactions", Matchers.hasSize(0));
    }

    @Test
    public void getTransactionsForTransactionShouldReturnHttpNotFoundIfParentTranscationDoesNotExist() {
        given().port(port)
                .contentType(JSON)
                .get("/v1/transaction/some-parent-external-id/transaction?gateway_account_id=1")
                .then()
                .contentType(JSON)
                .statusCode(Response.Status.NOT_FOUND.getStatusCode())
                .body("message", is("Transaction with id [some-parent-external-id] not found"));
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
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.SUBMITTED)
                .withGatewayAccountId(otherGatewayAccountId)
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withTransactionType("REFUND")
                .withParentExternalId(transactionFixture.getExternalId())
                .withGatewayAccountId(transactionFixture.getGatewayAccountId())
                .withRefundedByUserEmail("refund-by-user-email@example.org")
                .withCreatedDate(ZonedDateTime.parse("2018-03-12T16:24:01.123456Z"))
                .withAmount(100L)
                .withTotalAmount(100L)
                .withState(TransactionState.ERROR_GATEWAY)
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi());

        metadataKeyDao.insertIfNotExist("test-key-1");
        metadataKeyDao.insertIfNotExist("test-key-2");

        transactionMetadataDao.insertIfNotExist(transactionFixture.getId(), "test-key-1");

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

        assertHealthyCsvResponse(csvResponseStream, transactionFixture);
    }

    @Test
    public void shouldReturnCSVHeadersInCorrectOrder() throws IOException {
        String targetGatewayAccountId = "123";

        aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withGatewayAccountId(targetGatewayAccountId)
                .insert(rule.getJdbi());

        InputStream csvResponseStream = given().port(port)
                .accept("text/csv")
                .get("/v1/transaction?" +
                        "account_id=" + targetGatewayAccountId +
                        "&page=1" +
                        "&display_size=5"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType("text/csv")
                .extract().asInputStream();

        List<CSVRecord> csvRecords = CSVParser.parse(csvResponseStream, Charset.defaultCharset(), DEFAULT).getRecords();

        CSVRecord header = csvRecords.get(0);
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
        assertThat(header.get(20), is("Card Type"));
    }

    public void assertHealthyCsvResponse(InputStream csvStream, TransactionFixture transactionFixture) throws IOException {
        List<CSVRecord> csvRecords = CSVParser.parse(csvStream, Charset.defaultCharset(), RFC4180.withFirstRecordAsHeader()).getRecords();

        assertThat(csvRecords.size(), is(2));

        CSVRecord paymentRecord = csvRecords.get(0);
        assertPaymentTransactionDetails(paymentRecord, transactionFixture);
        assertThat(paymentRecord.get("Amount"), is("1.23"));
        assertThat(paymentRecord.get("State"), is("Error"));
        assertThat(paymentRecord.get("Finished"), is("true"));
        assertThat(paymentRecord.get("Error Code"), is("P0050"));
        assertThat(paymentRecord.get("Error Message"), is("Payment provider returned an error"));
        assertThat(paymentRecord.get("Date Created"), is("12 Mar 2018"));
        assertThat(paymentRecord.get("Time Created"), is("16:25:01"));
        assertThat(paymentRecord.get("Corporate Card Surcharge"), is("0.05"));
        assertThat(paymentRecord.get("Total Amount"), is("1.23"));
        assertThat(paymentRecord.get("test-key-1 (metadata)"), is("value1"));
        assertThat(paymentRecord.get("Wallet Type"), is(""));

        CSVRecord refundRecord = csvRecords.get(1);
        assertPaymentTransactionDetails(refundRecord, transactionFixture);
        assertThat(refundRecord.get("Amount"), is("-1.00"));
        assertThat(refundRecord.get("State"), is("Refund error"));
        assertThat(refundRecord.get("Finished"), is("true"));
        assertThat(refundRecord.get("Error Code"), is("P0050"));
        assertThat(refundRecord.get("Error Message"), is("Payment provider returned an error"));
        assertThat(refundRecord.get("Date Created"), is("12 Mar 2018"));
        assertThat(refundRecord.get("Time Created"), is("16:24:01"));
        assertThat(refundRecord.get("Corporate Card Surcharge"), is("0.00"));
        assertThat(refundRecord.get("Total Amount"), is("-1.00"));
        assertThat(refundRecord.get("Wallet Type"), is(""));
        assertThat(refundRecord.get("Issued By"), is("refund-by-user-email@example.org"));
    }

    private void assertPaymentTransactionDetails(CSVRecord csvRecord, TransactionFixture transactionFixture) {
        assertThat(csvRecord.get("Reference"), is(transactionFixture.getReference()));
        assertThat(csvRecord.get("Description"), is(transactionFixture.getDescription()));
        assertThat(csvRecord.get("Email"), is("someone@example.org"));
        assertThat(csvRecord.get("Card Brand"), is("Diners Club"));
        assertThat(csvRecord.get("Cardholder Name"), is("J Doe"));
        assertThat(csvRecord.get("Card Expiry Date"), is("10/21"));
        assertThat(csvRecord.get("Card Number"), is("1234"));
        assertThat(csvRecord.get("Provider ID"), is("gateway-transaction-id"));
        assertThat(csvRecord.get("GOV.UK Payment ID"), is(transactionFixture.getExternalId()));
        assertThat(csvRecord.get("Card Type"), is("credit"));
    }
}
