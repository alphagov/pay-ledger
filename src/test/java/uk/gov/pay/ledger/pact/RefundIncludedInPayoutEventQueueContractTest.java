package uk.gov.pay.ledger.pact;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.rule.SqsTestDocker;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.QueueRefundEventFixture;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class RefundIncludedInPayoutEventQueueContractTest {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;
    private String refundExternalId = "refund-external-id";
    private String payoutId = "po_12345";
    private ZonedDateTime eventDate = ZonedDateTime.parse("2018-03-12T16:25:01.123456Z");
    private QueueRefundEventFixture refundFixture;

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createRefundIncludedInPayoutEventPact(MessagePactBuilder builder) {
        refundFixture = QueueRefundEventFixture.aQueueRefundEventFixture()
                .withResourceExternalId(refundExternalId)
                .withEventDate(eventDate)
                .withEventData(String.format("{\"gateway_payout_id\": \"%s\"}", payoutId))
                .withParentResourceExternalId(null)
                .withEventType("REFUND_INCLUDED_IN_PAYOUT");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a refund included in payout message")
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
        String parentExternalId = "payment-id";
        aTransactionFixture()
                .withTransactionType(TransactionType.PAYMENT.name())
                .withExternalId(parentExternalId)
                .insert(appRule.getJdbi());
        aTransactionFixture()
                .withTransactionType(TransactionType.REFUND.name())
                .withExternalId(refundExternalId)
                .withParentExternalId(parentExternalId)
                .insert(appRule.getJdbi());

        appRule.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), new String(currentMessage));

        TransactionDao transactionDao = new TransactionDao(appRule.getJdbi());
        EventDao eventDao = appRule.getJdbi().onDemand(EventDao.class);

        await().atMost(1, TimeUnit.SECONDS).until(
                () -> transactionDao.findTransactionByExternalId(refundExternalId)
                        .map(transactionEntity -> transactionEntity.getGatewayPayoutId() != null)
                        .orElse(false)
        );

        Optional<TransactionEntity> transaction = transactionDao.findTransactionByExternalId(refundExternalId);
        assertThat(transaction.isPresent(), is(true));
        assertThat(transaction.get().getGatewayPayoutId(), is(payoutId));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
