package uk.gov.pay.ledger.transaction.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.transaction.model.Payment;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.pay.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.PayoutFixture.PayoutFixtureBuilder.aPayoutFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aPersistedTransactionList;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class TransactionResourceSearchIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private Integer port = rule.getAppRule().getLocalPort();

    @BeforeEach
    public void setUp() {
        aDatabaseTestHelper(rule.getJdbi()).truncateAllData();
    }

    @Test
    public void shouldSearchUsingAllFieldsAndReturnAllFieldsCorrectly() {
        String gatewayAccountId = randomAlphanumeric(20);
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
                .body("results[0].refund_summary.amount_available", is(transactionToVerify.getRefundSummary().getAmountAvailable().intValue()))
                .body("results[0].refund_summary.amount_submitted", is(transactionToVerify.getRefundSummary().getAmountSubmitted().intValue()))
                .body("results[0].refund_summary.amount_refunded", is(transactionToVerify.getRefundSummary().getAmountRefunded().intValue()))
                .body("results[0].settlement_summary.settled_date", is(nullValue()))

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
    public void shouldSearchTransactionWithLimitAppliedOnTotal_WhenLimitTotalFlagIsSet() {
        String gatewayAccountId = "gateway-account-id" + randomAlphanumeric(5);
        aPersistedTransactionList(gatewayAccountId, 100, rule.getJdbi(), true);

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?" +
                        "account_id=" + gatewayAccountId +
                        "&limit_total=true" +
                        "&limit_total_size=10" +
                        "&display_size=5"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("total", is(10))
                .body("count", is(5));
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
    public void shouldSearchUsingMultipleGatewayAccountIds() {
        String targetGatewayAccountId = "123";
        String targetGatewayAccountId2 = "456";
        String targetGatewayAccountId3 = "1337";

        TransactionFixture targetPayment = aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.SUBMITTED)
                .withGatewayAccountId(targetGatewayAccountId)
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.SUBMITTED)
                .withGatewayAccountId(targetGatewayAccountId2)
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withState(TransactionState.SUBMITTED)
                .withGatewayAccountId(targetGatewayAccountId3)
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?" +
                        "account_id=" + targetGatewayAccountId + "," + targetGatewayAccountId2 +
                        "&page=1" +
                        "&display_size=5"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(2));
    }

    @Test
    public void shouldReturnSettledDateForSearch() {
        String gatewayAccountId = "12345678";
        String gatewayPayoutId = "payout-id";

        aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId)
                .insert(rule.getJdbi());
        aPayoutFixture()
                .withGatewayPayoutId(gatewayPayoutId)
                .withGatewayAccountId(gatewayAccountId)
                .withPaidOutDate(ZonedDateTime.now())
                .build()
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?" +
                        "account_id=" + gatewayAccountId +
                        "&page=1" +
                        "&display_size=5"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(1))
                .body("results[0].settlement_summary.settled_date", is(notNullValue()));
    }

    @Test
    public void shouldReturnSettledDateForSearchBySettledDate() {
        String gatewayAccountId = randomAlphanumeric(10);
        String gatewayPayoutId1 = randomAlphanumeric(20);
        String gatewayPayoutId2 = randomAlphanumeric(20);
        String gatewayPayoutId3 = randomAlphanumeric(20);
        String gatewayPayoutId4 = randomAlphanumeric(20);

        aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId1)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId2)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withTransactionType("PAYMENT")
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId3)
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withTransactionType("REFUND")
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId4)
                .insert(rule.getJdbi());

        aPayoutFixture()
                .withGatewayPayoutId(gatewayPayoutId1)
                .withGatewayAccountId(gatewayAccountId)
                .withPaidOutDate(ZonedDateTime.parse("2020-09-07T00:00:01Z"))
                .build()
                .insert(rule.getJdbi());
        aPayoutFixture()
                .withGatewayPayoutId(gatewayPayoutId2)
                .withGatewayAccountId(gatewayAccountId)
                .withPaidOutDate(ZonedDateTime.parse("2020-09-06T23:59:59.999Z"))
                .build()
                .insert(rule.getJdbi());
        aPayoutFixture()
                .withGatewayPayoutId(gatewayPayoutId3)
                .withGatewayAccountId(gatewayAccountId)
                .withPaidOutDate(ZonedDateTime.parse("2020-09-09T00:00:00Z"))
                .build()
                .insert(rule.getJdbi());
        aPayoutFixture()
                .withGatewayPayoutId(gatewayPayoutId4)
                .withGatewayAccountId(gatewayAccountId)
                .withPaidOutDate(ZonedDateTime.parse("2020-09-08T23:59:59Z"))
                .build()
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?" +
                        "account_id=" + gatewayAccountId +
                        "&from_settled_date=2020-09-07" +
                        "&to_settled_date=2020-09-08" +
                        "&page=1" +
                        "&display_size=5"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(2))
                .body("results[0].settlement_summary.settled_date", is("2020-09-08"))
                .body("results[1].settlement_summary.settled_date", is("2020-09-07"))
                .body("_links.self.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_settled_date=2020-09-07&to_settled_date=2020-09-08&page=1&display_size=5"))
                .body("_links.first_page.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_settled_date=2020-09-07&to_settled_date=2020-09-08&page=1&display_size=5"))
                .body("_links.last_page.href", containsString("v1/transaction?account_id=" + gatewayAccountId + "&from_settled_date=2020-09-07&to_settled_date=2020-09-08&page=1&display_size=5"))
                .body("_links.prev_page.href", is(nullValue()))
                .body("_links.next_page.href", is(nullValue()));
    }
}
