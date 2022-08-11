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
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.payout.state.PayoutState.IN_TRANSIT;

public class PayoutCreatedEventQueueConsumerIT {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;
    private String gatewayPayoutId = "po_1234567890";
    private ZonedDateTime eventDate = ZonedDateTime.parse("2020-05-13T18:45:00.000000Z");

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createPayoutCreatedEventPact(MessagePactBuilder builder) {
        QueuePayoutEventFixture payoutEventFixture = QueuePayoutEventFixture.aQueuePayoutEventFixture()
                .withResourceExternalId("po_1234567890")
                .withEventDate(eventDate)
                .withDefaultEventDataForEventType("PAYOUT_CREATED");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a payout created message")
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
        assertThat(payoutEntity.get().getCreatedDate().toString(), is("2020-05-13T18:45Z"));
        assertThat(payoutEntity.get().getGatewayPayoutId(), is(gatewayPayoutId));
        assertThat(payoutEntity.get().getGatewayAccountId(), is("123456789"));
        assertThat(payoutEntity.get().getAmount(), is(1000L));
        assertThat(payoutEntity.get().getState(), is(IN_TRANSIT));

        Map<String, Object> payoutDetails = new Gson().fromJson(payoutEntity.get().getPayoutDetails(), Map.class);

        assertThat(payoutDetails.get("estimated_arrival_date_in_bank"), is("2020-05-13T18:45:33.000000Z"));
        assertThat(payoutDetails.get("gateway_status"), is("pending"));
        assertThat(payoutDetails.get("destination_type"), is("bank_account"));
        assertThat(payoutDetails.get("statement_descriptor"), is("SERVICE NAME"));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
