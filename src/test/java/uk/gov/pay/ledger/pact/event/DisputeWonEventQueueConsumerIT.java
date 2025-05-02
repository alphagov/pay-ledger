package uk.gov.pay.ledger.pact.event;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.junit.MessagePactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.entity.EventEntity;
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
import static uk.gov.pay.ledger.transaction.state.TransactionState.WON;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class DisputeWonEventQueueConsumerIT {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private GsonBuilder gsonBuilder = new GsonBuilder();
    private byte[] currentMessage;
    private String resourceExternalId = "dispute-won-external-id";
    private final String gatewayAccountId = "a-gateway-account-id";
    private final String parentsResourceExternalId = "parent-extid-dispute-won";
    private final String serviceId = "service-id";
    private final ZonedDateTime eventDate = ofInstant(ofEpochSecond(1642579160L), UTC);

    private QueueDisputeEventFixture eventFixture;

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createDisputeWonEventPact(MessagePactBuilder builder) {
        String eventData = gsonBuilder.create()
                .toJson(Map.of(
                        "gateway_account_id", gatewayAccountId
                ));
        eventFixture = QueueDisputeEventFixture.aQueueDisputeEventFixture()
                .withLive(true)
                .withResourceExternalId(resourceExternalId)
                .withParentResourceExternalId(parentsResourceExternalId)
                .withEventDate(eventDate)
                .withServiceId(serviceId)
                .withEventData(eventData)
                .withEventType("DISPUTE_WON")
                .withServiceId(serviceId);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a dispute won event")
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
        SendMessageRequest messageRequest = SendMessageRequest.builder()
                                .queueUrl(SqsTestDocker.getQueueUrl("event-queue"))
                                .messageBody(new String(currentMessage))
                                .build();
        appRule.getSqsClient().sendMessage(messageRequest);
        EventDao eventDao = appRule.getJdbi().onDemand(EventDao.class);
        await().atMost(1, TimeUnit.SECONDS).until(
                () -> !eventDao.findEventsForExternalIds(Set.of(resourceExternalId)).isEmpty()
        );

        List<EventEntity> events = eventDao.findEventsForExternalIds(Set.of(resourceExternalId));
        assertThat(events.isEmpty(), is(false));
        EventEntity event = events.get(0);
        assertThat(event.getEventType(), is("DISPUTE_WON"));
        assertThat(event.getEventDate(), is(eventDate));
        assertThat(event.getResourceType(), is(DISPUTE));
        assertThat(event.getResourceExternalId(), is(resourceExternalId));
        assertThat(event.getParentResourceExternalId(), is(parentsResourceExternalId));

        JsonNode eventData = new ObjectMapper().readTree(event.getEventData());
        assertThat(eventData.get("gateway_account_id").asText(), is(gatewayAccountId));

        TransactionDao transactionDao = new TransactionDao(appRule.getJdbi(), mock(LedgerConfig.class));

        Optional<TransactionEntity> transaction = transactionDao.findTransactionByExternalId(resourceExternalId);
        assertThat(transaction.isPresent(), is(true));
        assertThat(transaction.get().getExternalId(), is(resourceExternalId));
        assertThat(transaction.get().getParentExternalId(), is(parentsResourceExternalId));
        assertThat(transaction.get().getState(), is(WON));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
