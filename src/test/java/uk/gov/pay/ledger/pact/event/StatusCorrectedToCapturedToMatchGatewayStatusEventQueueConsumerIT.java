package uk.gov.pay.ledger.pact.event;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit.MessagePactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.google.gson.Gson;
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


public class StatusCorrectedToCapturedToMatchGatewayStatusEventQueueConsumerIT {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;
    private String externalId = "captureConfirm_externalId";
    private ZonedDateTime eventDate = ZonedDateTime.parse("2018-03-12T16:25:01.123456Z");
    private String gatewayAccountId = "gateway_account_id";

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createCaptureConfirmedEventPact(MessagePactBuilder builder) {
        String eventType = "STATUS_CORRECTED_TO_CAPTURED_TO_MATCH_GATEWAY_STATUS";
        QueuePaymentEventFixture statusCorrectedToCapturedEvent = QueuePaymentEventFixture.aQueuePaymentEventFixture()
                .withResourceExternalId(externalId)
                .withEventDate(eventDate)
                .withGatewayAccountId(gatewayAccountId)
                .withEventType(eventType)
                .withDefaultEventDataForEventType(eventType);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        PactDslJsonBody pactBody = statusCorrectedToCapturedEvent.getAsPact();

        return builder
                .expectsToReceive("a status corrected to captured event")
                .withMetadata(metadata)
                .withContent(pactBody)
                .toPact();
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
                () -> transactionDao.findTransactionByExternalId(externalId).isPresent()
        );

        Optional<TransactionEntity> transaction = transactionDao.findTransactionByExternalId(externalId);
        assertThat(transaction.isPresent(), is(true));
        assertThat(transaction.get().getExternalId(), is(externalId));
        assertThat(transaction.get().getFee(), is(5L));
        assertThat(transaction.get().getNetAmount(), is(1069L));

        Map<String, Object> transactionDetails = new Gson().fromJson(transaction.get().getTransactionDetails(), Map.class);
        assertThat(transactionDetails.get("captured_date"), is("2018-03-12T16:25:01.123456Z"));

        EventEntity event = eventDao.getEventsByResourceExternalId(externalId).get(0);
        Map<String, Object> eventDetails = new Gson().fromJson(event.getEventData(), Map.class);
        assertThat(eventDetails.get("gateway_event_date"),  is("2018-03-12T16:25:01.123456Z"));
        assertThat(eventDetails.get("captured_date"),  is("2018-03-12T16:25:01.123456Z"));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
