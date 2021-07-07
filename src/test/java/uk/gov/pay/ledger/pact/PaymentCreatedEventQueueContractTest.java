package uk.gov.pay.ledger.pact;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.ledger.event.dao.EventDao;
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

public class PaymentCreatedEventQueueContractTest {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;
    private String externalId = "created_externalId";
    private ZonedDateTime eventDate = ZonedDateTime.parse("2018-03-12T16:25:01.123456Z");
    private String gatewayAccountId = "gateway_account_id";
    private String credentialExternalId = "credential-external-id-1";

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createPaymentCreatedEventPact(MessagePactBuilder builder) {
        QueuePaymentEventFixture paymentCreatedEventFixture = QueuePaymentEventFixture.aQueuePaymentEventFixture()
                .withResourceExternalId(externalId)
                .withEventDate(eventDate)
                .withGatewayAccountId(gatewayAccountId)
                .withCredentialExternalId(credentialExternalId)
                .withDefaultEventDataForEventType("PAYMENT_CREATED");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a payment created message")
                .withMetadata(metadata)
                .withContent(paymentCreatedEventFixture.getAsPact())
                .toPact();
    }

    @Test
    @PactVerification({"connector"})
    public void test() {
        appRule.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), new String(currentMessage));

        TransactionDao transactionDao = new TransactionDao(appRule.getJdbi());
        EventDao eventDao = appRule.getJdbi().onDemand(EventDao.class);

        await().atMost(1, TimeUnit.SECONDS).until(
                () -> transactionDao.findTransactionByExternalIdAndGatewayAccountId(externalId, gatewayAccountId).isPresent()
        );

        Optional<TransactionEntity> transaction = transactionDao.findTransactionByExternalIdAndGatewayAccountId(externalId, gatewayAccountId);
        assertThat(transaction.isPresent(), is(true));
        assertThat(transaction.get().getExternalId(), is(externalId));
        assertThat(transaction.get().getAmount(), is(1000L));
        assertThat(transaction.get().getDescription(), is("a description"));
        assertThat(transaction.get().getReference(), is("aref"));
        assertThat(transaction.get().getGatewayAccountId(), is(gatewayAccountId));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"return_url\": \"https://example.org\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"payment_provider\": \"sandbox\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"language\": \"en\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"delayed_capture\": false"));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"external_metadata\": {\"key\": \"value\"}"));
        assertThat(transaction.get().getEmail(), is("j.doe@example.org"));
        assertThat(transaction.get().getCardholderName(), is("J citizen"));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"address_line1\": \"12 Rouge Avenue\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"address_postcode\": \"N1 3QU\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"address_city\": \"London\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"address_country\": \"GB\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"credential_external_id\": \"" + credentialExternalId + "\""));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
