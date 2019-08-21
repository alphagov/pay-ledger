package uk.gov.pay.ledger.pact;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.rule.SqsTestDocker;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.QueueRefundEventFixture;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.fixture.QueueRefundEventFixture.aQueueRefundEventFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class RefundSubmittedEventQueueContractTest {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;
    private QueueRefundEventFixture refundFixture = aQueueRefundEventFixture()
            .withResourceType(ResourceType.REFUND)
            .withEventType("REFUND_SUBMITTED")
            .withDefaultEventDataForEventType("REFUND_SUBMITTED");

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createRefundSubmittedEventPact(MessagePactBuilder builder) {
        Map<String, String> metadata = new HashMap();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a refund submitted message")
                .withMetadata(metadata)
                .withContent(refundFixture.getAsPact())
                .toPact();
    }

    @Before
    public void setUp() {
        DatabaseTestHelper.aDatabaseTestHelper(appRule.getJdbi()).truncateAllData();
    }

    @Test
    @PactVerification({"connector"})
    public void test() {
        aTransactionFixture()
                .withExternalId(refundFixture.getParentResourceExternalId())
                .insert(appRule.getJdbi());

        appRule.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), new String(currentMessage));
        TransactionDao transactionDao = new TransactionDao(appRule.getJdbi());

        await().atMost(1, TimeUnit.SECONDS).until(
                () -> transactionDao.findTransactionByExternalId(refundFixture.getResourceExternalId()).isPresent()
        );

        Optional<TransactionEntity> transaction = transactionDao.findTransactionByExternalId(refundFixture.getResourceExternalId());

        assertThat(transaction.get().getExternalId(), is(refundFixture.getResourceExternalId()));
        assertThat(transaction.get().getParentExternalId(),is(refundFixture.getParentResourceExternalId()));
        assertThat(transaction.get().getCreatedDate(),is(refundFixture.getEventDate()));
        assertThat(transaction.get().getTransactionDetails(), is("{}"));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
