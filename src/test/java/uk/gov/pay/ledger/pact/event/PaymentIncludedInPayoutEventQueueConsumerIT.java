package uk.gov.pay.ledger.pact.event;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.junit.MessagePactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.rule.SqsTestDocker;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class PaymentIncludedInPayoutEventQueueConsumerIT {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;
    private String paymentExternalId = "payment-external-id";
    private String payoutId = "po_1234567890";
    private ZonedDateTime eventDate = ZonedDateTime.parse("2018-03-12T16:25:01.123456Z");

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createPaymentIncludedInPayoutEventPact(MessagePactBuilder builder) {
        QueuePaymentEventFixture eventFixture = QueuePaymentEventFixture.aQueuePaymentEventFixture()
                .withResourceExternalId(paymentExternalId)
                .withEventDate(eventDate)
                .withServiceId(null)
                .withEventData(String.format("{\"gateway_payout_id\": \"%s\"}", payoutId))
                .withEventType("PAYMENT_INCLUDED_IN_PAYOUT");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a payment included in payout message")
                .withMetadata(metadata)
                .withContent(eventFixture.getAsPact())
                .toPact();
    }

    @Before
    public void setUp() {
        DatabaseTestHelper.aDatabaseTestHelper(appRule.getJdbi()).truncateAllData();
    }

    @Test
    @PactVerification({"connector"})
    public void test() {
        SendMessageRequest messageRequest = SendMessageRequest.builder()
                .queueUrl(SqsTestDocker.getQueueUrl("event-queue"))
                .messageBody(new String(currentMessage))
                .build();
        appRule.getSqsClient().sendMessage(messageRequest);

        TransactionDao transactionDao = new TransactionDao(appRule.getJdbi(), mock(LedgerConfig.class));
        EventDao eventDao = appRule.getJdbi().onDemand(EventDao.class);

        await().atMost(1, TimeUnit.SECONDS).until(
                () -> transactionDao.findTransactionByExternalId(paymentExternalId).isPresent()
        );

        Optional<TransactionEntity> transaction = transactionDao.findTransactionByExternalId(paymentExternalId);
        assertThat(transaction.isPresent(), is(true));
        assertThat(transaction.get().getGatewayPayoutId(), is(payoutId));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
