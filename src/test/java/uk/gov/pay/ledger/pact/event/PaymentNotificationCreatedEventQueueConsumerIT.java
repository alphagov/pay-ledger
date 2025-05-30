package uk.gov.pay.ledger.pact.event;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.junit.MessagePactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.event.dao.EventDao;
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

public class PaymentNotificationCreatedEventQueueConsumerIT {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;
    private String externalId = "notifications_externalId";
    private ZonedDateTime eventDate = ZonedDateTime.parse("2018-03-12T16:25:01.123456Z");
    private String gatewayAccountId = "gateway_account_id";
    private String credentialExternalId = "credential-external-id-1";

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createPaymentNotificationCreatedEventPact(MessagePactBuilder builder) {
        String paymentDetailsEnteredEventName = "PAYMENT_NOTIFICATION_CREATED";
        QueuePaymentEventFixture paymentNotificationCreatedEventFixture = QueuePaymentEventFixture.aQueuePaymentEventFixture()
                .withResourceExternalId(externalId)
                .withEventDate(eventDate)
                .withGatewayAccountId(gatewayAccountId)
                .withCredentialExternalId(credentialExternalId)
                .withEventType(paymentDetailsEnteredEventName)
                .withDefaultEventDataForEventType(paymentDetailsEnteredEventName)
                .withLive(true);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a payment notification created message")
                .withMetadata(metadata)
                .withContent(paymentNotificationCreatedEventFixture.getAsPact())
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
        assertThat(transaction.get().getEmail(), is("j.doe@example.org"));
        assertThat(transaction.get().getCardBrand(), is("visa"));
        assertThat(transaction.get().getFirstDigitsCardNumber(), is("424242"));
        assertThat(transaction.get().getLastDigitsCardNumber(), is("4242"));
        assertThat(transaction.get().getCardholderName(), is("Mr Test"));
        assertThat(transaction.get().getReference(), is("This is a reference"));
        assertThat(transaction.get().getCardBrand(), is("visa"));
        assertThat(transaction.get().getDescription(), is("This is a description"));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"gateway_transaction_id\": \"providerId\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"amount\": 1000"));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"expiry_date\": \"12/99\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"status\": \"success\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"auth_code\": \"012345\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"processor_id\": \"processorId\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"telephone_number\": \"+447700900796\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"authorised_date\": \"2018-02-21T16:05:33Z\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"created_date\": \"2018-02-21T15:05:13Z\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"card_brand_label\": \"Visa\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"payment_provider\": \"sandbox\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"expiry_date\": \"12/99\""));
        assertThat(transaction.get().getTransactionDetails(), containsString("\"credential_external_id\": \"" + credentialExternalId + "\""));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
