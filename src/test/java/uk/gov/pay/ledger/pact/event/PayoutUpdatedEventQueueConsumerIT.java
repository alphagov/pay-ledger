package uk.gov.pay.ledger.pact.event;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.junit.MessagePactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.google.gson.Gson;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.ledger.payout.dao.PayoutDao;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.rule.SqsTestDocker;
import uk.gov.pay.ledger.util.fixture.QueuePayoutEventFixture;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.time.ZonedDateTime.parse;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.payout.state.PayoutState.UNDEFINED;

public class PayoutUpdatedEventQueueConsumerIT {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;
    private String gatewayPayoutId = "po_updated_1234567890";
    private ZonedDateTime eventDate = parse("2020-05-13T18:50:00Z");
    private String eventType = "PAYOUT_UPDATED";

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createPayoutUpdatedEventPact(MessagePactBuilder builder) {
        QueuePayoutEventFixture payoutEventFixture = QueuePayoutEventFixture.aQueuePayoutEventFixture()
                .withResourceExternalId(gatewayPayoutId)
                .withEventDate(eventDate)
                .withEventType(eventType)
                .withDefaultEventDataForEventType(eventType);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a payout updated message")
                .withMetadata(metadata)
                .withContent(payoutEventFixture.getAsPact())
                .toPact();
    }

    @Test
    @PactVerification({"connector"})
    public void test() {
        appRule.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), new String(currentMessage));
        PayoutDao payoutDao = new PayoutDao(appRule.getJdbi());

        await().atMost(1, TimeUnit.SECONDS).until(
                () -> payoutDao.findByGatewayPayoutId(gatewayPayoutId).isPresent()
        );

        Optional<PayoutEntity> payoutEntity = payoutDao.findByGatewayPayoutId(gatewayPayoutId);
        assertThat(payoutEntity.isPresent(), is(true));
        assertThat(payoutEntity.get().getGatewayPayoutId(), is(gatewayPayoutId));
        assertThat(payoutEntity.get().getState(), is(UNDEFINED));

        Map<String, Object> payoutDetails = new Gson().fromJson(payoutEntity.get().getPayoutDetails(), Map.class);
        assertThat(payoutDetails.get("gateway_status"), is("pending"));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
