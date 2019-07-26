package uk.gov.pay.ledger.pact;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.rule.SqsTestDocker;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class CaptureConfirmedEventQueueContractTest {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;
    private String externalId = "externalId";
    private ZonedDateTime eventDate = ZonedDateTime.parse("2018-03-12T16:25:01.123456Z");
    private String gatewayAccountId = "gateway_account_id";

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createCaptureConfirmedEventPact(MessagePactBuilder builder) {
        String eventType = "CAPTURE_CONFIRMED";
        QueuePaymentEventFixture captureConfirmedEvent = QueuePaymentEventFixture.aQueuePaymentEventFixture()
                .withResourceExternalId(externalId)
                .withEventDate(eventDate)
                .withGatewayAccountId(gatewayAccountId)
                .withEventType(eventType)
                .withDefaultEventDataForEventType(eventType);

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("contentType", "application/json");

        PactDslJsonBody pactBody = captureConfirmedEvent.getAsPact();

        return builder
                .expectsToReceive("a capture confirmed message")
                .withMetadata(metadata)
                .withContent(pactBody)
                .toPact();
    }

    @Test
    @PactVerification({"connector"})
    public void test() throws Exception {
        appRule.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), new String(currentMessage));

        TransactionDao transactionDao = new TransactionDao(appRule.getJdbi());
        EventDao eventDao = appRule.getJdbi().onDemand(EventDao.class);

        await().atMost(1, TimeUnit.SECONDS).until(
                () -> transactionDao.findTransactionByExternalId(externalId).isPresent()
        );

        Optional<TransactionEntity> transaction = transactionDao.findTransactionByExternalId(externalId);
        assertThat(transaction.isPresent(), is(true));
        assertThat(transaction.get().getExternalId(), is(externalId));
        assertThat(transaction.get().getFee(), is(5L));
        assertThat(transaction.get().getNetAmount(), is(1069L));

        Event event = eventDao.getEventsByResourceExternalId(externalId).get(0);
        assertThat(event.getEventData(), containsString("\"gateway_event_date\": \"2018-03-12T16:25:01.123456Z\""));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
