package uk.gov.pay.ledger.queue;

import com.google.gson.Gson;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.transaction.model.TransactionType.PAYMENT;
import static uk.gov.pay.ledger.transaction.state.TransactionState.SUCCESS;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

@ExtendWith(DropwizardExtensionsSupport.class)
public class QueueMessageReceiverIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    TransactionDao transactionDao = new TransactionDao(rule.getJdbi());
    private DatabaseTestHelper dbHelper = aDatabaseTestHelper(rule.getJdbi());

    private static final ZonedDateTime CREATED_AT = ZonedDateTime.parse("2019-06-07T08:46:01.123456Z");

    @Test
    public void shouldHandleOutOfOrderEvents() throws InterruptedException {
        final String resourceExternalId = "rexid";
        String gatewayAccountId = "test_gateway_account_id";
        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("AUTHORISATION_SUCCEEDED")
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        // A created event with an earlier timestamp, sent later
        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT.minusMinutes(1))
                .withGatewayAccountId(gatewayAccountId)
                .withEventType(SalientEventType.PAYMENT_CREATED.name())
                .withDefaultEventDataForEventType(SalientEventType.PAYMENT_CREATED.name())
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(200)
                .body("transaction_id", is(resourceExternalId))
                .body("state.status", is("submitted"));
    }

    @Test
    public void shouldContinueToHandleMessagesFromQueueForDownstreamExceptions() throws InterruptedException {
        final String resourceExternalId = "rexid";
        final String resourceExternalId2 = "rexid2";
        String gatewayAccountId = "test_gateway_account_id";

        aQueuePaymentEventFixture()
                .withResourceType(ResourceType.SERVICE) // throws PSQL exception. change to UNKNOWN when 'service' events are allowed
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("AUTHORISATION_SUCCEEDED")
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        QueuePaymentEventFixture secondResource = aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId2)
                .withResourceType(ResourceType.PAYMENT)
                .withEventDate(CREATED_AT.minusMinutes(1))
                .withEventType(SalientEventType.PAYMENT_CREATED.name())
                .withDefaultEventDataForEventType(SalientEventType.PAYMENT_CREATED.name())
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(404);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId2 + "?account_id=" + secondResource.getGatewayAccountId())
                .then()
                .statusCode(200)
                .body("transaction_id", is(resourceExternalId2))
                .body("state.status", is("created"));
    }

    @Test
    public void shouldHandleRefundEvent() throws InterruptedException {
        final String resourceExternalId = "rexid";
        final String parentResourceExternalId = "parentRexId";
        final String gatewayAccountId = "test_accountId";

        aQueuePaymentEventFixture()
                .withResourceExternalId(parentResourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("PAYMENT_CREATED")
                .withDefaultEventDataForEventType("PAYMENT_CREATED")
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId(parentResourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("PAYMENT_DETAILS_ENTERED")
                .withDefaultEventDataForEventType("PAYMENT_DETAILS_ENTERED")
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId(parentResourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("AUTHORISATION_SUCCEEDED")
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        Thread.sleep(100);
        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withParentResourceExternalId(parentResourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("REFUND_CREATED_BY_SERVICE")
                .withResourceType(ResourceType.REFUND)
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        //todo: replace with refunds endpoint when available
        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(200)
                .body("transaction_id", is(resourceExternalId))
                .body("state.status", is("submitted"));

        Optional<TransactionEntity> mayBeRefund = transactionDao.findTransactionByExternalId(resourceExternalId);
        TransactionEntity refund = mayBeRefund.get();
        assertThat(refund.getCardholderName(), is("J citizen"));
        assertThat(refund.getEmail(), is("j.doe@example.org"));
        assertThat(refund.getCardBrand(), is("visa"));
        assertThat(refund.getDescription(), is("a description"));
        assertThat(refund.getLastDigitsCardNumber(), is("4242"));
        assertThat(refund.getFirstDigitsCardNumber(), is("424242"));
        assertThat(refund.getReference(), is("aref"));
        Map<String, Object> transactionDetails = new Gson().fromJson(refund.getTransactionDetails(), Map.class);
        Map<String, String> paymentDetails = (Map<String, String>) transactionDetails.get("payment_details");

        assertThat(paymentDetails.get("card_brand_label"), is("Visa"));
        assertThat(paymentDetails.get("expiry_date"), is("11/21"));
        assertThat(paymentDetails.get("card_type"), is("DEBIT"));
    }

    @Test
    public void shouldHandleRefundAvailabilityUpdatedEvent() throws InterruptedException {
        final String resourceExternalId = "rexid2";
        final String gatewayAccountId = "test_accountId";
        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("AUTHORISATION_SUCCEEDED")
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("REFUND_AVAILABILITY_UPDATED")
                .withEventData("{\"refund_amount_available\": 200}")
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(200)
                .body("transaction_id", is(resourceExternalId))
                .body("refund_summary.amount_available", is(200));
    }

    @Test
    public void shouldHandleForceCaptureEvent() throws InterruptedException {
        final String resourceExternalId = "rexid3";
        final String gatewayAccountId = "test_accountId";
        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("AUTHORISATION_REJECTED")
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(200)
                .body("transaction_id", is(resourceExternalId))
                .body("state.status", is("declined"));

        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("CAPTURE_CONFIRMED_BY_GATEWAY_NOTIFICATION")
                .withEventData(format("{\"gateway_account_id\":\"%s\"}", gatewayAccountId))
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(200)
                .body("transaction_id", is(resourceExternalId))
                .body("state.status", is("success"));

    }

    @Test
    public void shouldProjectTransactionSummaryForPaymentEvents() throws InterruptedException {
        final String resourceExternalId = "rexid" + randomAlphanumeric(10);
        final String gatewayAccountId = "test_accountId" + randomAlphanumeric(10);

        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT.plusSeconds(1))
                .withEventType("CAPTURE_SUBMITTED")
                .withEventData("{}")
                .withDefaultEventDataForEventType("DEFAULT")
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT.plusSeconds(2))
                .withEventType("CAPTURE_CONFIRMED")
                .withDefaultEventDataForEventType("CAPTURE_CONFIRMED")
                .insert(rule.getSqsClient());

        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withGatewayAccountId(gatewayAccountId)
                .withEventDate(CREATED_AT)
                .withEventType("PAYMENT_CREATED")
                .withDefaultEventDataForEventType("PAYMENT_CREATED")
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        List<Map<String, Object>> transactionSummary = dbHelper.getTransactionSummary(gatewayAccountId, PAYMENT, SUCCESS, LocalDate.parse("2019-06-07"), true, false);

        assertThat(transactionSummary.get(0).get("gateway_account_id"), is(gatewayAccountId));
        assertThat(transactionSummary.get(0).get("transaction_date").toString(), Matchers.is("2019-06-07"));
        assertThat(transactionSummary.get(0).get("state"), is("SUCCESS"));
        assertThat(transactionSummary.get(0).get("live"), is(true));
        assertThat(transactionSummary.get(0).get("moto"), is(false));
        assertThat(transactionSummary.get(0).get("total_amount_in_pence"), is(1000L));
        assertThat(transactionSummary.get(0).get("no_of_transactions"), is(1L));
        assertThat(transactionSummary.get(0).get("total_fee_in_pence"), is(5L));
    }
}
