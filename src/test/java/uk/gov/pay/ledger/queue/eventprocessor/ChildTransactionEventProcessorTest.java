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
import uk.gov.pay.ledger.event.model.EventEntity;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.exception.EmptyEventsException;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.event.model.ResourceType.REFUND;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

@ExtendWith(MockitoExtension.class)
class ChildTransactionEventProcessorTest {

    @Mock
    private EventService mockEventService;
    @Mock
    private TransactionService mockTransactionService;
    @Captor
    private ArgumentCaptor<TransactionEntity> transactionEntityArgumentCaptor;

    private TransactionEntityFactory transactionEntityFactory;
    private ChildTransactionEventProcessor childTransactionEventProcessor;

    @BeforeEach
    void setUp() {
        transactionEntityFactory = new TransactionEntityFactory(new ObjectMapper());
        childTransactionEventProcessor = new ChildTransactionEventProcessor(mockEventService, mockTransactionService,
                transactionEntityFactory);
    }

    @Test
    void shouldProjectRefundTransactionWithPaymentInformation() {
        String refundExternalId = "refund-external-id";
        String paymentExternalId = "payment-external-id";

        EventEntity refundEvent = anEventFixture()
                .withResourceType(REFUND)
                .withEventType("REFUND_SUCCEEDED")
                .withEventDate(ZonedDateTime.parse("2022-07-01T10:00:00Z"))
                .withResourceExternalId(refundExternalId)
                .withParentResourceExternalId(paymentExternalId)
                .withEventData(new GsonBuilder().create()
                        .toJson(ImmutableMap.builder()
                                .put("amount", 99)
                                .put("gateway_transaction_id", "a-gateway-id")
                                .build()))
                .toEntity();

        EventEntity paymentEvent1 = aQueuePaymentEventFixture()
                .withResourceExternalId(paymentExternalId)
                .withEventType("PAYMENT_CREATED")
                .withEventDate(ZonedDateTime.parse("2022-06-01T10:00:00Z"))
                .withDefaultEventDataForEventType("PAYMENT_CREATED")
                .toEntity();
        EventEntity paymentEvent2 = aQueuePaymentEventFixture()
                .withResourceExternalId(paymentExternalId)
                .withEventType("PAYMENT_DETAILS_ENTERED")
                .withEventDate(ZonedDateTime.parse("2022-06-01T10:01:00Z"))
                .withDefaultEventDataForEventType("PAYMENT_DETAILS_ENTERED")
                .toEntity();
        EventEntity paymentEvent3 = aQueuePaymentEventFixture()
                .withResourceExternalId(paymentExternalId)
                .withEventType("FEE_INCURRED")
                .withEventDate(ZonedDateTime.parse("2022-06-01T10:02:00Z"))
                .withEventData(new GsonBuilder().create()
                        .toJson(Map.of(
                                "net_amount", 900,
                                "fee", 100
                        )))
                .toEntity();

        EventDigest refundEventDigest = EventDigest.fromEventList(List.of(refundEvent));
        when(mockEventService.getEventDigestForResource(refundEvent)).thenReturn(refundEventDigest);
        EventDigest paymentEventDigest = EventDigest.fromEventList(List.of(paymentEvent1, paymentEvent2, paymentEvent3));
        when(mockEventService.getEventDigestForResource(paymentExternalId)).thenReturn(paymentEventDigest);

        childTransactionEventProcessor.process(refundEvent, true);

        verify(mockTransactionService).upsertTransaction(transactionEntityArgumentCaptor.capture());

        TransactionEntity transactionEntity = transactionEntityArgumentCaptor.getValue();
        // these fields come from the refund events
        assertThat(transactionEntity.getAmount(), is(99L));
        assertThat(transactionEntity.getGatewayTransactionId(), is("a-gateway-id"));
        assertThat(transactionEntity.getNetAmount(), is(nullValue()));
        assertThat(transactionEntity.getTotalAmount(), is(nullValue()));
        assertThat(transactionEntity.getFee(), is(nullValue()));

        // these fields come from the payment events
        assertThat(transactionEntity.getReference(), is(paymentEventDigest.getEventAggregate().get("reference")));
        assertThat(transactionEntity.getCardBrand(), is(paymentEventDigest.getEventAggregate().get("card_brand")));
        assertThat(transactionEntity.getCardholderName(), is(paymentEventDigest.getEventAggregate().get("cardholder_name")));
        assertThat(transactionEntity.getFirstDigitsCardNumber(), is(paymentEventDigest.getEventAggregate().get("first_digits_card_number")));
        assertThat(transactionEntity.getLastDigitsCardNumber(), is(paymentEventDigest.getEventAggregate().get("last_digits_card_number")));
        assertThat(transactionEntity.getDescription(), is(paymentEventDigest.getEventAggregate().get("description")));
        assertThat(transactionEntity.getEmail(), is(paymentEventDigest.getEventAggregate().get("email")));

        JsonObject transactionDetails = JsonParser.parseString(transactionEntity.getTransactionDetails()).getAsJsonObject();
        assertThat(transactionDetails.get("payment_details").getAsJsonObject(), is(notNullValue()));
        assertThat(transactionDetails.get("payment_details").getAsJsonObject().get("card_type").getAsString(), is("DEBIT"));
    }

    @Test
    void shouldProjectRefundTransactionWithoutPaymentInformationIfUnavailable() {
        String refundExternalId = "refund-external-id";
        String paymentExternalId = "payment-external-id";

        EventEntity refundEvent = anEventFixture()
                .withResourceType(REFUND)
                .withEventType("REFUND_SUCCEEDED")
                .withResourceExternalId(refundExternalId)
                .withParentResourceExternalId(paymentExternalId)
                .toEntity();

        EventDigest refundEventDigest = EventDigest.fromEventList(List.of(refundEvent));
        when(mockEventService.getEventDigestForResource(refundEvent)).thenReturn(refundEventDigest);
        when(mockEventService.getEventDigestForResource(paymentExternalId)).thenThrow(EmptyEventsException.class);

        childTransactionEventProcessor.process(refundEvent, true);

        verify(mockTransactionService).upsertTransactionFor(refundEventDigest);
    }

    @Test
    void shouldIncludePaymentInformationWhenChildTransactionReprojected() {

        String paymentEventData = new GsonBuilder().create()
                .toJson(ImmutableMap.builder()
                        .put("amount", 100)
                        .put("reference", "payment-ref")
                        .put("card_type", "visa")
                        .build());
        EventEntity paymentEvent = anEventFixture().withEventData(paymentEventData).toEntity();
        EventDigest paymentEventDigest = EventDigest.fromEventList(List.of(paymentEvent));

        String refundEventData = new GsonBuilder().create()
                .toJson(ImmutableMap.builder()
                        .put("amount", -50)
                        .put("some_refund_info", "blah")
                        .build());
        EventEntity refundEvent = anEventFixture().withEventData(refundEventData).toEntity();
        EventDigest refundEventDigest = EventDigest.fromEventList(List.of(refundEvent));

        String refundExternalId = "refund-external-id";
        when(mockEventService.getEventDigestForResource(refundExternalId)).thenReturn(refundEventDigest);

        childTransactionEventProcessor.reprojectChildTransaction(refundExternalId, paymentEventDigest);

        verify(mockTransactionService).upsertTransaction(transactionEntityArgumentCaptor.capture());

        TransactionEntity transactionEntity = transactionEntityArgumentCaptor.getValue();
        assertThat(transactionEntity.getReference(), is("payment-ref"));
        assertThat(transactionEntity.getAmount(), is(-50L));

        JsonObject transactionDetails = JsonParser.parseString(transactionEntity.getTransactionDetails()).getAsJsonObject();
        assertThat(transactionDetails.get("some_refund_info").getAsString(), is("blah"));
        assertThat(transactionDetails.get("payment_details").getAsJsonObject(), is(notNullValue()));
        assertThat(transactionDetails.get("payment_details").getAsJsonObject().get("card_type").getAsString(), is("visa"));
    }


    @Test
    void shouldProjectDisputeTransaction() {
        EventEntity disputeEvent = anEventFixture().withResourceType(ResourceType.DISPUTE).withLive(true).toEntity();
        EventDigest eventDigest = EventDigest.fromEventList(List.of(disputeEvent));
        when(mockEventService.getEventDigestForResource(disputeEvent)).thenReturn(eventDigest);
        childTransactionEventProcessor.process(disputeEvent, true);

        verify(mockTransactionService).upsertTransactionFor(eventDigest);
    }
}
