package uk.gov.pay.ledger.pact.event;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.ledger.app.LedgerConfig;
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
import static org.mockito.Mockito.mock;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

public class GatewayRequires3dsAuthorisationEventQueueConsumerIT {

    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;
    private String externalId = "requires_3ds_externalId";
    private ZonedDateTime eventDate = ZonedDateTime.parse("2021-09-09T13:40:01.123456Z");

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createGatewayRequires3dsAuthorisationEventPact(MessagePactBuilder builder) {
        String gatewayRequires3dsAuthorisationEvent = "GATEWAY_REQUIRES_3DS_AUTHORISATION";
        QueuePaymentEventFixture gatewayRequires3dsAuthorisation = aQueuePaymentEventFixture()
                .withResourceExternalId(externalId)
                .withEventDate(eventDate)
                .withEventType(gatewayRequires3dsAuthorisationEvent)
                .withDefaultEventDataForEventType(gatewayRequires3dsAuthorisationEvent)
                .withLive(true);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a gateway requires 3DS authorisation message")
                .withMetadata(metadata)
                .withContent(gatewayRequires3dsAuthorisation.getAsPact())
                .toPact();
    }

    @Test
    @PactVerification({"connector"})
    public void test() {
        TransactionDao transactionDao = new TransactionDao(appRule.getJdbi(), mock(LedgerConfig.class));

        appRule.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), new String(currentMessage));

        await().atMost(1, TimeUnit.SECONDS).until(
                () -> transactionDao.findTransactionByExternalId(externalId).isPresent()
                        && transactionDao.findTransactionByExternalId(externalId).get().getTransactionDetails().contains("\"requires_3ds\""));

        Optional<TransactionEntity> transaction = transactionDao.findTransactionByExternalId(externalId);

        assertThat(transaction.isPresent(), is(true));
        assertThat(transaction.get().getExternalId(), is(externalId));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"version_3ds\": \"1.2.1\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"requires_3ds\": true"));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
