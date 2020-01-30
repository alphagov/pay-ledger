package uk.gov.pay.ledger.queue;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;

import java.time.ZonedDateTime;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.pay.commons.model.Source.CARD_API;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

public class QueueMessageReceiverForTransactionIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule(config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true"));

    private final String gatewayAccountId = "test_gateway_account_id";

    @Test
    public void paymentCreatedMessageIsPersistedCorrectly() throws InterruptedException {
        String resourceExternalId = "rexid";
        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(ZonedDateTime.parse("2020-01-30T08:46:01.123456Z"))
                .withGatewayAccountId(gatewayAccountId)
                .withEventType(SalientEventType.PAYMENT_CREATED.name())
                .withEventData(createEventData())
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(200)
                .body("transaction_id", is(resourceExternalId))
                .body("moto", is(true));
    }

    private String createEventData() {
        return new GsonBuilder().create()
                .toJson(ImmutableMap.builder()
                        .put("amount", 1000)
                        .put("description", "a description")
                        .put("language", "en")
                        .put("reference", "aref")
                        .put("return_url", "https://example.org")
                        .put("gateway_account_id", gatewayAccountId)
                        .put("payment_provider", "sandbox")
                        .put("delayed_capture", false)
                        .put("moto", true)
                        .put("live", true)
                        .put("email", "j.doe@example.org")
                        .put("cardholder_name", "J citizen")
                        .put("address_line1", "12 Rouge Avenue")
                        .put("address_postcode", "N1 3QU")
                        .put("address_city", "London")
                        .put("source", CARD_API)
                        .put("address_country", "GB")
                        .build());
    }
}
