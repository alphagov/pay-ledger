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
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.service.TransactionService;
import uk.gov.pay.ledger.util.fixture.EventFixture;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;

@ExtendWith(MockitoExtension.class)
class RefundEventProcessorTest {

    @Mock
    private EventService eventService;
    @Mock
    private TransactionService transactionService;
    @Captor
    private ArgumentCaptor<TransactionEntity> transactionEntityArgumentCaptor;

    private TransactionEntityFactory transactionEntityFactory;
    private RefundEventProcessor refundEventProcessor;

    @BeforeEach
    void setUp() {
        transactionEntityFactory = new TransactionEntityFactory(new ObjectMapper());
        refundEventProcessor = new RefundEventProcessor(eventService, transactionService, transactionEntityFactory);
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

        refundEventProcessor.reprojectRefundTransaction(refundExternalId, paymentEventDigest);

        verify(transactionService).upsertTransaction(transactionEntityArgumentCaptor.capture());

        TransactionEntity transactionEntity = transactionEntityArgumentCaptor.getValue();
        assertThat(transactionEntity.getReference(), is("payment-ref"));
        assertThat(transactionEntity.getAmount(), is(-50L));

        JsonObject transactionDetails = JsonParser.parseString(transactionEntity.getTransactionDetails()).getAsJsonObject();
        assertThat(transactionDetails.get("some_refund_info").getAsString(), is("blah"));
        assertThat(transactionDetails.get("payment_details").getAsJsonObject(), is(notNullValue()));
        assertThat(transactionDetails.get("payment_details").getAsJsonObject().get("card_type").getAsString(), is("visa"));
    }
}