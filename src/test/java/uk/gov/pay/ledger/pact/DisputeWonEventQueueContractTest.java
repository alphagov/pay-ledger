package uk.gov.pay.ledger.pact;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.rule.SqsTestDocker;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.QueueDisputeEventFixture;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.time.Instant.ofEpochSecond;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.event.model.ResourceType.DISPUTE;

public class DisputeWonEventQueueContractTest {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private GsonBuilder gsonBuilder = new GsonBuilder();
    private byte[] currentMessage;
    private String paymentExternalId = "payment-external-id";
    private final String gatewayAccountId = "a-gateway-account-id";
    private final String resourceExternalId = paymentExternalId;
    private final String parentsResourceExternalId = "external-id";
    private final String serviceId = "service-id";
    private final ZonedDateTime disputeCreated = ofInstant(ofEpochSecond(1642579160L), UTC);

    private QueueDisputeEventFixture eventFixture;

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createDisputeWonEventPact(MessagePactBuilder builder) {
        String eventData = gsonBuilder.create()
                .toJson(Map.of(
                        "gateway_account_id", gatewayAccountId
                ));
        eventFixture = QueueDisputeEventFixture.aQueueDisputeEventFixture()
                .withLive(true)
                .withResourceExternalId(paymentExternalId)
                .withParentResourceExternalId(parentsResourceExternalId)
                .withEventDate(disputeCreated)
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
        assertThat(event.getEventType(), is("DISPUTE_WON"));
        assertThat(event.getEventDate(), is(disputeCreated));
        assertThat(event.getResourceType(), is(DISPUTE));
        assertThat(event.getResourceExternalId(), is(resourceExternalId));
        assertThat(event.getParentResourceExternalId(), is(parentsResourceExternalId));

        JsonNode eventData = new ObjectMapper().readTree(event.getEventData());
        assertThat(eventData.get("gateway_account_id").asText(), is(gatewayAccountId));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}