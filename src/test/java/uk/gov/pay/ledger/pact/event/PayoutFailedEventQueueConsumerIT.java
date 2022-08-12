package uk.gov.pay.ledger.pact.event;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.model.v3.messaging.MessagePact;
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
import static uk.gov.pay.ledger.payout.state.PayoutState.FAILED;

public class PayoutFailedEventQueueConsumerIT {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;
    private String gatewayPayoutId = "po_failed_1234567890";
    private ZonedDateTime eventDate = parse("2020-05-13T18:50:00Z");
    private String eventType = "PAYOUT_FAILED";

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createPayoutFailedEventPact(MessagePactBuilder builder) {
        QueuePayoutEventFixture payoutEventFixture = QueuePayoutEventFixture.aQueuePayoutEventFixture()
                .withResourceExternalId(gatewayPayoutId)
                .withEventDate(eventDate)
                .withEventType(eventType)
                .withDefaultEventDataForEventType(eventType);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a payout failed message")
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
        assertThat(payoutEntity.get().getState(), is(FAILED));

        Map<String, Object> payoutDetails = new Gson().fromJson(payoutEntity.get().getPayoutDetails(), Map.class);
        assertThat(payoutDetails.get("gateway_status"), is("failed"));
        assertThat(payoutDetails.get("failure_code"), is("account_closed"));
        assertThat(payoutDetails.get("failure_message"), is("The bank account has been closed"));
        assertThat(payoutDetails.get("failure_balance_transaction"), is("ba_aaaaaaaaaa"));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
