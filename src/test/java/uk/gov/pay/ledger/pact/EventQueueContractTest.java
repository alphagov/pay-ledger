package uk.gov.pay.ledger.pact;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.rule.SqsTestDocker;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EventQueueContractTest {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createPaymentCreatedEventPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();

        PactDslJsonBody paymentCreatedEventDetails = new PactDslJsonBody();
        paymentCreatedEventDetails.numberType("amount", 1000);
        paymentCreatedEventDetails.stringType("description", "payment description");
        paymentCreatedEventDetails.stringType("reference", "payment reference");
        paymentCreatedEventDetails.stringType("return_url", "https://example.com");
        paymentCreatedEventDetails.stringType("payment_provider", "stripe");

        body.stringType("event_type", "PAYMENT_CREATED");
        body.stringType("timestamp", ZonedDateTime.now(ZoneOffset.UTC).toString());
        body.stringType("resource_external_id", "externalId");
        body.stringType("resource_type", "payment");
        body.object("event_details", paymentCreatedEventDetails);

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a payment created message")
                .withContent(body)
                .toPact();
    }

    @Test
    @PactVerification({"connector"})
    public void test() throws Exception {
        appRule.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), new String(currentMessage));

        DatabaseTestHelper dbHelper = DatabaseTestHelper.aDatabaseTestHelper(appRule.getJdbi());
        Thread.sleep(1000L);

        var event = dbHelper.getEventByExternalId("externalId");
        assertThat(event.get("resource_external_id"), is("externalId"));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
