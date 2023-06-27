package uk.gov.pay.ledger.pact.event;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.rule.SqsTestDocker;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;
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
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

public class PaymentDetailsTakenFromPaymentInstrumentEventQueueConsumerIT {
    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule appRule = new AppWithPostgresAndSqsRule(
            config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true")
    );

    private byte[] currentMessage;
    private String externalId = "jweojfewjoifewj";
    private ZonedDateTime eventDate = ZonedDateTime.parse("2023-06-27T14:12:01.123456Z");

    @Pact(provider = "connector", consumer = "ledger")
    public MessagePact createPaymentDetailsTakenFromPaymentInstrumentEventPact(MessagePactBuilder builder) {
        String paymentDetailsTakenFromPaymentInstrumentEventName = "PAYMENT_DETAILS_TAKEN_FROM_PAYMENT_INSTRUMENT";
        QueuePaymentEventFixture paymentDetailsTakenFromPaymentInstrument = aQueuePaymentEventFixture()
                .withResourceExternalId(externalId)
                .withResourceType(ResourceType.PAYMENT)
                .withEventDate(eventDate)
                .withEventType(paymentDetailsTakenFromPaymentInstrumentEventName)
                .withGatewayAccountId("3456")
                .withDefaultEventDataForEventType(paymentDetailsTakenFromPaymentInstrumentEventName)
                .withLive(true);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a payment details taken from payment instrument message")
                .withMetadata(metadata)
                .withContent(paymentDetailsTakenFromPaymentInstrument.getAsPact())
                .toPact();
    }

    @Test
    @PactVerification({"connector"})
    public void test() {
        TransactionDao transactionDao = new TransactionDao(appRule.getJdbi(), mock(LedgerConfig.class));
        setupTransaction(transactionDao);

        appRule.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), new String(currentMessage));

        await().atMost(1, TimeUnit.SECONDS).until(
                () -> transactionDao.findTransactionByExternalId(externalId).isPresent()
                        && transactionDao.findTransactionByExternalId(externalId).get().getCardholderName() != null
        );

        Optional<TransactionEntity> optionalTransaction = transactionDao.findTransactionByExternalId(externalId);

        assertThat(optionalTransaction.isPresent(), is(true));

        TransactionEntity transaction = optionalTransaction.get();

        assertThat(transaction.getExternalId(), is(externalId));
        assertThat(transaction.getCardBrand(), is("visa"));
        assertThat(transaction.getFirstDigitsCardNumber(), is("123456"));
        assertThat(transaction.getLastDigitsCardNumber(), is("1234"));
        assertThat(transaction.getCardholderName(), is("Test"));
        assertThat(transaction.getState(), is(TransactionState.CREATED));
        assertThat(transaction.getTransactionDetails(), containsString("\"expiry_date\": \"11/99\""));
        assertThat(transaction.getTransactionDetails(), containsString("\"address_line1\": \"10 WCB\""));
        assertThat(transaction.getTransactionDetails(), containsString("\"address_postcode\": \"E1 8XX\""));
        assertThat(transaction.getTransactionDetails(), containsString("\"address_city\": \"London\""));
        assertThat(transaction.getTransactionDetails(), containsString("\"address_country\": \"UK\""));
        assertThat(transaction.getTransactionDetails(), containsString("\"gateway_transaction_id\": \"a-provider-transaction-id\""));
    }

    private void setupTransaction(TransactionDao transactionDao) {
        aQueuePaymentEventFixture()
                .withEventType("PAYMENT_CREATED")
                .withResourceExternalId(externalId)
                .withEventDate(eventDate.minusHours(1))
                .insert(appRule.getSqsClient());
        await().atMost(1, TimeUnit.SECONDS).until(
                () -> transactionDao.findTransactionByExternalId(externalId).isPresent()
        );
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
