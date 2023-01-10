package uk.gov.pay.ledger.pact.event;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
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
import static uk.gov.pay.ledger.transaction.state.TransactionState.NEEDS_RESPONSE;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class DisputeCreatedEventQueueConsumerIT {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    Gson gson = new Gson();

    private byte[] currentMessage;
    private String resourceExternalId = "dispute-created-ext-id";
    private final long amount = 6500L;
    private final String reason = "duplicate";
    private final String gatewayAccountId = "a-gateway-account-id";
    private final String parentsResourceExternalId = "parent-extid-disp-created";
    private final String serviceId = "service-id";
    private final ZonedDateTime eventDate = ofInstant(ofEpochSecond(1642579160L), UTC);

    private QueueDisputeEventFixture eventFixture;

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createDisputeCreatedEventPact(MessagePactBuilder builder) {
        eventFixture = QueueDisputeEventFixture.aQueueDisputeEventFixture()
                .withLive(true)
                .withResourceExternalId(resourceExternalId)
                .withParentResourceExternalId(parentsResourceExternalId)
                .withEventDate(eventDate)
                .withServiceId(serviceId)
                .withDefaultEventDataForEventType(SalientEventType.DISPUTE_CREATED.name())
                .withEventType("DISPUTE_CREATED")
                .withServiceId(serviceId);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a dispute created event")
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

        List<Event> events = eventDao.findEventsForExternalIds(Set.of(resourceExternalId));
        assertThat(events.isEmpty(), is(false));
        Event event = events.get(0);
        assertThat(event.getEventType(), is("DISPUTE_CREATED"));
        assertThat(event.getEventDate(), is(eventDate));
        assertThat(event.getResourceType(), is(DISPUTE));
        assertThat(event.getResourceExternalId(), is(resourceExternalId));
        assertThat(event.getParentResourceExternalId(), is(parentsResourceExternalId));

        JsonNode eventData = new ObjectMapper().readTree(event.getEventData());
        assertThat(eventData.get("amount").asLong(), is(amount));
        assertThat(eventData.get("gateway_account_id").asText(), is(gatewayAccountId));
        assertThat(eventData.get("reason").asText(), is(reason));
        assertThat(eventData.get("evidence_due_date").asText(), is("2022-02-14T23:59:59.000Z"));

        TransactionDao transactionDao = new TransactionDao(appRule.getJdbi(), mock(LedgerConfig.class));

        Optional<TransactionEntity> transaction = transactionDao.findTransactionByExternalId(resourceExternalId);
        assertThat(transaction.isPresent(), is(true));
        assertThat(transaction.get().getExternalId(), is(resourceExternalId));
        assertThat(transaction.get().getParentExternalId(), is(parentsResourceExternalId));
        assertThat(transaction.get().getGatewayAccountId(), is("a-gateway-account-id"));
        assertThat(transaction.get().getAmount(), is(6500L));
        assertThat(transaction.get().getState(), is(NEEDS_RESPONSE));

        Map<String, String> transactionDetails = gson.fromJson(transaction.get().getTransactionDetails(), Map.class);
        assertThat(transactionDetails.get("reason"), is("duplicate"));
        assertThat(transactionDetails.get("evidence_due_date"), is("2022-02-14T23:59:59.000Z"));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
