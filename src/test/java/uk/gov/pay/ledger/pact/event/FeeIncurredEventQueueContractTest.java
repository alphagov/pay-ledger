package uk.gov.pay.ledger.pact.event;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class FeeIncurredEventQueueContractTest {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private GsonBuilder gsonBuilder = new GsonBuilder();
    private byte[] currentMessage;
    private String paymentExternalId = "payment-external-id";
    private ZonedDateTime eventDate = ZonedDateTime.parse("2018-03-12T16:25:01.123456Z");

    public static final long fee = 33L;
    public static final long netAmount = 67L;

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createFeeIncurredEventPact(MessagePactBuilder builder) {
        String eventData = gsonBuilder.create()
                .toJson(Map.of(
                        "fee", fee,
                        "net_amount", netAmount,
                        "fee_breakdown", List.of(
                                Map.of(
                                        "amount", 24,
                                        "fee_type", "transaction"
                                )
                        )
                ));
        QueuePaymentEventFixture eventFixture = QueuePaymentEventFixture.aQueuePaymentEventFixture()
                .withResourceExternalId(paymentExternalId)
                .withEventDate(eventDate)
                .withServiceId(null)
                .withEventData(eventData)
                .withEventType("FEE_INCURRED");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a fee incurred message")
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
        appRule.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), new String(currentMessage));

        TransactionDao transactionDao = new TransactionDao(appRule.getJdbi(), mock(LedgerConfig.class));

        await().atMost(1, TimeUnit.SECONDS).until(
                () -> transactionDao.findTransactionByExternalId(paymentExternalId).isPresent()
        );

        Optional<TransactionEntity> transaction = transactionDao.findTransactionByExternalId(paymentExternalId);
        assertThat(transaction.isPresent(), is(true));
        assertThat(transaction.get().getFee(), is(fee));
        assertThat(transaction.get().getNetAmount(), is(netAmount));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"fee_breakdown\": [{\"amount\": 24, \"fee_type\": \"transaction\"}]}"));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
