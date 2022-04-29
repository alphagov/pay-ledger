package uk.gov.pay.ledger.pact;

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

public class PaymentDetailsSubmittedByAPIEventQueueContractTest {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;
    private String externalId = "paymentDetails_externalId";
    private ZonedDateTime eventDate = ZonedDateTime.parse("2018-03-12T16:25:01.123456Z");

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createPaymentDetailsSubmittedByAPIEventPact(MessagePactBuilder builder) {
        String paymentDetailsSubmittedByAPIEventName = "PAYMENT_DETAILS_SUBMITTED_BY_API";
        QueuePaymentEventFixture paymentDetailsSubmitted = aQueuePaymentEventFixture()
                .withResourceExternalId(externalId)
                .withEventDate(eventDate)
                .withEventType(paymentDetailsSubmittedByAPIEventName)
                .withGatewayAccountId("I0YI1")
                .withDefaultEventDataForEventType(paymentDetailsSubmittedByAPIEventName)
                .withLive(true);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a payment details submitted by api message")
                .withMetadata(metadata)
                .withContent(paymentDetailsSubmitted.getAsPact())
                .toPact();
    }

    @Test
    @PactVerification({"connector"})
    public void test() {
        TransactionDao transactionDao = new TransactionDao(appRule.getJdbi(), mock(LedgerConfig.class));
        setupTransaction(transactionDao);

        appRule.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), new String(currentMessage));

        await().atMost(1, TimeUnit.SECONDS).until(
                () -> transactionDao.findTransactionByExternalId(externalId).isPresent()
                        && transactionDao.findTransactionByExternalId(externalId).get().getCardholderName() != null
        );

        Optional<TransactionEntity> transaction = transactionDao.findTransactionByExternalId(externalId);

        assertThat(transaction.isPresent(), is(true));
        assertThat(transaction.get().getExternalId(), is(externalId));
        assertThat(transaction.get().getCardBrand(), is("visa"));
        assertThat(transaction.get().getFirstDigitsCardNumber(), is("424242"));
        assertThat(transaction.get().getLastDigitsCardNumber(), is("4242"));
        assertThat(transaction.get().getCardholderName(), is("J citizen"));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"expiry_date\": \"11/21\""));
    }

    private void setupTransaction(TransactionDao transactionDao) {
        aQueuePaymentEventFixture()
                .withEventType("PAYMENT_CREATED")
                .withResourceExternalId(externalId)
                .withEventDate(eventDate.minusHours(1))
                .insert(appRule.getSqsClient());
        await().atMost(1, TimeUnit.SECONDS).until(
                () -> transactionDao.findTransactionByExternalId(externalId).isPresent()
        );
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}