package uk.gov.pay.ledger.pact;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.rule.SqsTestDocker;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.QueueEventFixture;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.awaitility.Awaitility.await;

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
        QueueEventFixture paymentCreatedEventFixture = QueueEventFixture.aQueueEventFixture()
                .withResourceExternalId("externalId")
                .withDefaultEventDataForEventType("PAYMENT_CREATED");

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a payment created message")
                .withContent(paymentCreatedEventFixture.getAsPact())
                .toPact();
    }

    @Test
    @PactVerification({"connector"})
    public void test() throws Exception {
        appRule.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), new String(currentMessage));

        DatabaseTestHelper dbHelper = DatabaseTestHelper.aDatabaseTestHelper(appRule.getJdbi());
        await().atMost(1, TimeUnit.SECONDS).until(
                () -> {
                    Map<String, Object> event;
                    try {
                        event = dbHelper.getEventByExternalId("externalId");
                        return "externalId".equals(event.get("resource_external_id"));

                    } catch (NoSuchElementException e) {
                        return false;
                    }
                }
        );
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
