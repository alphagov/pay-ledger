package uk.gov.pay.ledger.queue.eventprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.QueueMessageReceiverConfig;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;

@ExtendWith(MockitoExtension.class)
class ChildTransactionEventProcessorTest {

    @Mock
    private EventService eventService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private LedgerConfig ledgerConfig;
    @Mock
    private Clock clock;
    @Captor
    private ArgumentCaptor<TransactionEntity> transactionEntityArgumentCaptor;

    private TransactionEntityFactory transactionEntityFactory;
    private ChildTransactionEventProcessor childTransactionEventProcessor;

    @BeforeEach
    void setUp() {
        transactionEntityFactory = new TransactionEntityFactory(new ObjectMapper());
        childTransactionEventProcessor = new ChildTransactionEventProcessor(eventService, transactionService,
                transactionEntityFactory, ledgerConfig, clock);
    }

    @Test
    void shouldIncludePaymentInformationWhenRefundTransactionReprojected() {

        String paymentEventData = new GsonBuilder().create()
                .toJson(ImmutableMap.builder()
                        .put("amount", 100)
                        .put("reference", "payment-ref")
                        .put("card_type", "visa")
                        .build());
        Event paymentEvent = anEventFixture().withEventData(paymentEventData).toEntity();
        EventDigest paymentEventDigest = EventDigest.fromEventList(List.of(paymentEvent));

        String refundEventData = new GsonBuilder().create()
                .toJson(ImmutableMap.builder()
                .put("amount", -50)
                .put("some_refund_info", "blah")
                .build());
        Event refundEvent = anEventFixture().withEventData(refundEventData).toEntity();
        EventDigest refundEventDigest = EventDigest.fromEventList(List.of(refundEvent));

        String refundExternalId = "refund-external-id";
        when(eventService.getEventDigestForResource(refundExternalId)).thenReturn(refundEventDigest);

        childTransactionEventProcessor.reprojectChildTransaction(refundExternalId, paymentEventDigest);

        verify(transactionService).upsertTransaction(transactionEntityArgumentCaptor.capture());

        TransactionEntity transactionEntity = transactionEntityArgumentCaptor.getValue();
        assertThat(transactionEntity.getReference(), is("payment-ref"));
        assertThat(transactionEntity.getAmount(), is(-50L));

        JsonObject transactionDetails = JsonParser.parseString(transactionEntity.getTransactionDetails()).getAsJsonObject();
        assertThat(transactionDetails.get("some_refund_info").getAsString(), is("blah"));
        assertThat(transactionDetails.get("payment_details").getAsJsonObject(), is(notNullValue()));
        assertThat(transactionDetails.get("payment_details").getAsJsonObject().get("card_type").getAsString(), is("visa"));
    }

    @Test
    void shouldNotProjectTestDisputeIfBeforeEnabledDate() {
        when(clock.instant()).thenReturn(Instant.parse("2022-07-08T00:00:00Z"));
        QueueMessageReceiverConfig mockQueueMessageReceiverConfig = mock(QueueMessageReceiverConfig.class);
        when(ledgerConfig.getQueueMessageReceiverConfig()).thenReturn(mockQueueMessageReceiverConfig);
        when(mockQueueMessageReceiverConfig.getProjectTestPaymentsDisputeEventsFromDate()).thenReturn(Instant.parse("2022-07-08T00:00:01Z"));

        Event disputeEvent = anEventFixture().withResourceType(ResourceType.DISPUTE).withLive(false).toEntity();
        when(eventService.getEventDigestForResource(disputeEvent)).thenReturn(EventDigest.fromEventList(List.of(disputeEvent)));
        childTransactionEventProcessor.process(disputeEvent, true);

        verify(transactionService, never()).upsertTransaction(any());
        verify(transactionService, never()).upsertTransactionFor(any());
    }

    @Test
    void shouldNotProjectLiveDisputeIfBeforeEnabledDate() {
        when(clock.instant()).thenReturn(Instant.parse("2022-07-08T00:00:00Z"));
        QueueMessageReceiverConfig mockQueueMessageReceiverConfig = mock(QueueMessageReceiverConfig.class);
        when(ledgerConfig.getQueueMessageReceiverConfig()).thenReturn(mockQueueMessageReceiverConfig);
        when(mockQueueMessageReceiverConfig.getProjectLivePaymentsDisputeEventsFromDate()).thenReturn(Instant.parse("2022-07-08T00:00:01Z"));

        Event disputeEvent = anEventFixture().withResourceType(ResourceType.DISPUTE).withLive(true).toEntity();
        when(eventService.getEventDigestForResource(disputeEvent)).thenReturn(EventDigest.fromEventList(List.of(disputeEvent)));
        childTransactionEventProcessor.process(disputeEvent, true);

        verify(transactionService, never()).upsertTransaction(any());
        verify(transactionService, never()).upsertTransactionFor(any());
    }

    @Test
    void shouldProjectTestDisputeIfAfterEnabledDate() {
        when(clock.instant()).thenReturn(Instant.parse("2022-07-08T00:00:01Z"));
        QueueMessageReceiverConfig mockQueueMessageReceiverConfig = mock(QueueMessageReceiverConfig.class);
        when(ledgerConfig.getQueueMessageReceiverConfig()).thenReturn(mockQueueMessageReceiverConfig);
        when(mockQueueMessageReceiverConfig.getProjectTestPaymentsDisputeEventsFromDate()).thenReturn(Instant.parse("2022-07-08T00:00:00Z"));

        Event disputeEvent = anEventFixture().withResourceType(ResourceType.DISPUTE).withLive(false).toEntity();
        EventDigest eventDigest = EventDigest.fromEventList(List.of(disputeEvent));
        when(eventService.getEventDigestForResource(disputeEvent)).thenReturn(eventDigest);
        childTransactionEventProcessor.process(disputeEvent, true);

        verify(transactionService).upsertTransactionFor(eventDigest);
    }

    @Test
    void shouldProjectLiveDisputeIfAfterEnabledDate() {
        when(clock.instant()).thenReturn(Instant.parse("2022-07-08T00:00:01Z"));
        QueueMessageReceiverConfig mockQueueMessageReceiverConfig = mock(QueueMessageReceiverConfig.class);
        when(ledgerConfig.getQueueMessageReceiverConfig()).thenReturn(mockQueueMessageReceiverConfig);
        when(mockQueueMessageReceiverConfig.getProjectLivePaymentsDisputeEventsFromDate()).thenReturn(Instant.parse("2022-07-08T00:00:00Z"));

        Event disputeEvent = anEventFixture().withResourceType(ResourceType.DISPUTE).withLive(true).toEntity();
        EventDigest eventDigest = EventDigest.fromEventList(List.of(disputeEvent));
        when(eventService.getEventDigestForResource(disputeEvent)).thenReturn(eventDigest);
        childTransactionEventProcessor.process(disputeEvent, true);

        verify(transactionService).upsertTransactionFor(eventDigest);
    }
}