package uk.gov.pay.ledger.queue;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;

import java.time.ZonedDateTime;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.pay.commons.model.Source.CARD_API;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

@ExtendWith(DropwizardExtensionsSupport.class)
class QueueMessageReceiverForTransactionIT {

    @RegisterExtension
    static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true"));

    private final String gatewayAccountId = "test_gateway_account_id";

    @ParameterizedTest
    @ValueSource(strings = {
            "true",
            "false",
            "null"
    })
    void paymentCreatedMessageIsPersistedCorrectly(Boolean moto) throws InterruptedException {
        String resourceExternalId = RandomStringUtils.random(10, true, true);
        aQueuePaymentEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventDate(ZonedDateTime.parse("2020-01-30T08:46:01.123456Z"))
                .withGatewayAccountId(gatewayAccountId)
                .withEventType(SalientEventType.PAYMENT_CREATED.name())
                .withEventData(createEventData(moto))
                .insert(rule.getSqsClient());

        Thread.sleep(500);

        given().port(rule.getAppRule().getLocalPort())
                .contentType(JSON)
                .get("/v1/transaction/" + resourceExternalId + "?account_id=" + gatewayAccountId)
                .then()
                .statusCode(200)
                .body("transaction_id", is(resourceExternalId))
                .body("moto", is(moto == null ? false : moto));
    }

    private String createEventData(Boolean moto) {
        ImmutableMap.Builder<Object, Object> eventData = ImmutableMap.builder()
                .put("amount", 1000)
                .put("description", "a description")
                .put("language", "en")
                .put("reference", "aref")
                .put("return_url", "https://example.org")
                .put("gateway_account_id", gatewayAccountId)
                .put("payment_provider", "sandbox")
                .put("delayed_capture", false)
                .put("live", true)
                .put("email", "j.doe@example.org")
                .put("cardholder_name", "J citizen")
                .put("address_line1", "12 Rouge Avenue")
                .put("address_postcode", "N1 3QU")
                .put("address_city", "London")
                .put("source", CARD_API)
                .put("address_country", "GB");

        if (moto != null) {
            eventData.put("moto", moto);
        }

        return new GsonBuilder().create().toJson(eventData.build());
    }
}
