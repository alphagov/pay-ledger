package uk.gov.pay.ledger.queue.eventprocessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.service.TransactionMetadataService;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.event.model.ResourceType.PAYMENT;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

@ExtendWith(MockitoExtension.class)
class PaymentEventProcessorTest {

    @Mock
    private EventService eventService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private TransactionMetadataService transactionMetadataService;
    @Mock
    private RefundEventProcessor refundEventProcessor;

    private PaymentEventProcessor paymentEventProcessor;

    @BeforeEach
    void setUp() {
        paymentEventProcessor = new PaymentEventProcessor(eventService, transactionService, transactionMetadataService, refundEventProcessor);
    }

    @Test
    void shouldReprojectRefundTransactionsWhenPaymentHasRefunds() {
        String paymentExternalId = "payment-external-id";
        Event event = anEventFixture().withResourceType(PAYMENT)
                .withResourceExternalId(paymentExternalId)
                .withEventType("ADMIN_MANUALLY_DID_AN_UPDATE")
                .withEventData("{\"reference\": \"payment-ref\"}")
                .toEntity();

        Event previousEvent = anEventFixture().withResourceType(PAYMENT)
                .withEventType("USER_APPROVED_FOR_CAPTURE")
                .toEntity();

        when(eventService.getEventsForResource(event.getResourceExternalId())).thenReturn(List.of(previousEvent, event));

        TransactionEntity refundTransaction1 = aTransactionFixture().withExternalId("refund-external-id-1").toEntity();
        TransactionEntity refundTransaction2 = aTransactionFixture().withExternalId("refund-external-id-2").toEntity();
        when(transactionService.getChildTransactions(paymentExternalId)).thenReturn(List.of(refundTransaction1, refundTransaction2));

        paymentEventProcessor.process(event);

        verify(transactionService).upsertTransactionFor(any(EventDigest.class));
        verify(transactionMetadataService).upsertMetadataFor(any(EventDigest.class));
        verify(refundEventProcessor).reprojectRefundTransaction(eq(refundTransaction1.getExternalId()), any(EventDigest.class));
        verify(refundEventProcessor).reprojectRefundTransaction(eq(refundTransaction2.getExternalId()), any(EventDigest.class));
    }

    @Test
    void shouldNotQueryForRefundsIfPaymentHasntBeenInSuccessState() {
        String paymentExternalId = "payment-external-id";
        Event event = anEventFixture().withResourceType(PAYMENT)
                .withResourceExternalId(paymentExternalId)
                .withEventType("ADMIN_MANUALLY_DID_AN_UPDATE")
                .withEventData("{\"reference\": \"payment-ref\"}")
                .toEntity();

        Event previousEvent = anEventFixture().withResourceType(PAYMENT)
                .withEventType("PAYMENT_STARTED")
                .toEntity();

        when(eventService.getEventsForResource(event.getResourceExternalId())).thenReturn(List.of(previousEvent, event));

        paymentEventProcessor.process(event);
        verify(transactionService).upsertTransactionFor(any(EventDigest.class));
        verify(transactionMetadataService).upsertMetadataFor(any(EventDigest.class));
        verify(transactionService, never()).getChildTransactions(any());
        verify(refundEventProcessor, never()).reprojectRefundTransaction(any(), any());
    }

    @Test
    void shouldNotQueryForRefundsIfNoEventData() {
        String paymentExternalId = "payment-external-id";
        Event event = anEventFixture().withResourceType(PAYMENT)
                .withResourceExternalId(paymentExternalId)
                .withEventType("ADMIN_MANUALLY_DID_AN_UPDATE")
                .withEventData("{}")
                .toEntity();

        Event previousEvent = anEventFixture().withResourceType(PAYMENT)
                .withEventType("USER_APPROVED_FOR_CAPTURE")
                .toEntity();

        when(eventService.getEventsForResource(event.getResourceExternalId())).thenReturn(List.of(previousEvent, event));

        paymentEventProcessor.process(event);
        verify(transactionService).upsertTransactionFor(any(EventDigest.class));
        verify(transactionMetadataService).upsertMetadataFor(any(EventDigest.class));
        verify(transactionService, never()).getChildTransactions(any());
        verify(refundEventProcessor, never()).reprojectRefundTransaction(any(), any());
    }
}