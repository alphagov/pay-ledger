package uk.gov.pay.ledger.pact.event;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.dao.entity.EventEntity;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.rule.SqsTestDocker;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.QueueDisputeEventFixture;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.time.Instant.ofEpochSecond;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static uk.gov.pay.ledger.event.model.ResourceType.DISPUTE;
import static uk.gov.pay.ledger.transaction.state.TransactionState.LOST;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class DisputeLostEventQueueConsumerIT {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;
    private String resourceExternalId = "dispute-lost-ext-id";
    private final long fee = 1500L;
    private final long amount = 6500L;
    // there is a bug in pact assert where a negative number is not recognised as an integer.
    // add net_amount to pact check once a fix is released
    private final long netAmount = -8000L;
    private final String gatewayAccountId = "a-gateway-account-id";
    private final String parentsResourceExternalId = "parent-extid-dispute-lost";
    private final String serviceId = "service-id";
    private final ZonedDateTime disputeCreated = ofInstant(ofEpochSecond(1642579160L), UTC);

    private QueueDisputeEventFixture eventFixture;

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createDisputeLostEventPact(MessagePactBuilder builder) {
        eventFixture = QueueDisputeEventFixture.aQueueDisputeEventFixture()
                .withLive(true)
                .withResourceExternalId(resourceExternalId)
                .withParentResourceExternalId(parentsResourceExternalId)
                .withEventDate(disputeCreated)
                .withServiceId(serviceId)
                .withDefaultEventDataForEventType(SalientEventType.DISPUTE_LOST.name())
                .withEventType("DISPUTE_LOST")
                .withServiceId(serviceId);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a dispute lost event")
                .withMetadata(metadata)
                .withContent(eventFixture.getAsPact())
                .toPact();
    }

    @Before
    public void setUp() {
        DatabaseTestHelper.aDatabaseTestHelper(appRule.getJdbi()).truncateAllData();
        aTransactionFixture().withExternalId(parentsResourceExternalId).insert(appRule.getJdbi());
    }

    @Test
    @PactVerification({"connector"})
    public void test() throws JsonProcessingException {
        appRule.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), new String(currentMessage));
        EventDao eventDao = appRule.getJdbi().onDemand(EventDao.class);
        await().atMost(1, TimeUnit.SECONDS).until(
                () -> !eventDao.findEventsForExternalIds(Set.of(resourceExternalId)).isEmpty()
        );

        List<EventEntity> events = eventDao.findEventsForExternalIds(Set.of(resourceExternalId));
        assertThat(events.isEmpty(), is(false));
        EventEntity event = events.get(0);
        assertThat(event.getEventType(), is("DISPUTE_LOST"));
        assertThat(event.getEventDate(), is(disputeCreated));
        assertThat(event.getResourceType(), is(DISPUTE));
        assertThat(event.getResourceExternalId(), is(resourceExternalId));
        assertThat(event.getParentResourceExternalId(), is(parentsResourceExternalId));

        JsonNode eventData = new ObjectMapper().readTree(event.getEventData());
        assertThat(eventData.get("amount").asLong(), is(amount));
        assertThat(eventData.get("fee").asLong(), is(fee));
        assertThat(eventData.get("gateway_account_id").asText(), is(gatewayAccountId));

        TransactionDao transactionDao = new TransactionDao(appRule.getJdbi(), mock(LedgerConfig.class));

        Optional<TransactionEntity> transaction = transactionDao.findTransactionByExternalId(resourceExternalId);
        assertThat(transaction.isPresent(), is(true));
        assertThat(transaction.get().getExternalId(), is(resourceExternalId));
        assertThat(transaction.get().getParentExternalId(), is(parentsResourceExternalId));
        assertThat(transaction.get().getAmount(), is(amount));
        assertThat(transaction.get().getFee(), is(fee));
        assertThat(transaction.get().getState(), is(LOST));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
