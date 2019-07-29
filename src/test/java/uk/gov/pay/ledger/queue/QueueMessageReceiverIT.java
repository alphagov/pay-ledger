package uk.gov.pay.ledger.queue;

import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture;

import java.time.ZonedDateTime;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

public class QueueMessageReceiverIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule(config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true"));

    private static final ZonedDateTime CREATED_AT = ZonedDateTime.parse("2019-06-07T08:46:01.123456Z");

    @Test
    public void shouldHandleOutOfOrderEvents() throws InterruptedException {
        final String resourceExternalId = "rexid";
        String gatewayAccountId = "test_gateway_account_id";
        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(CREATED_AT)
                .withEventType("AUTHORISATION_SUCCESSFUL")
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
                .body("charge_id", is(resourceExternalId))
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
                .withEventType("AUTHORISATION_SUCCESSFUL")
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
                .body("charge_id", is(resourceExternalId2))
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
                .withEventType("AUTHORISATION_SUCCESSFUL")
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
                .body("charge_id", is(resourceExternalId))
                .body("state.status", is("submitted"));
    }
}
