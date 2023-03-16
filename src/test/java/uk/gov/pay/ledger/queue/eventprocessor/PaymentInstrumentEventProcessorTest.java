package uk.gov.pay.ledger.queue.eventprocessor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.agreement.service.AgreementService;
import uk.gov.pay.ledger.event.entity.EventEntity;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.service.EventService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.event.model.ResourceType.PAYMENT_INSTRUMENT;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;

@ExtendWith(MockitoExtension.class)
class PaymentInstrumentEventProcessorTest {
    @Mock
    EventService mockEventService;

    @Mock
    AgreementService mockAgreementService;

    @InjectMocks
    PaymentInstrumentEventProcessor paymentInstrumentEventProcessor;

    @Test
    void shouldUpsertPaymentInstrument() {
        EventEntity event = anEventFixture().withResourceType(PAYMENT_INSTRUMENT).toEntity();
        var eventDigest = EventDigest.fromEventList(List.of(anEventFixture().toEntity()));
        when(mockEventService.getEventDigestForResource(event)).thenReturn(eventDigest);

        paymentInstrumentEventProcessor.process(event, true);

        verify(mockEventService).getEventDigestForResource(event);
        verify(mockAgreementService).upsertPaymentInstrumentFor(eventDigest);
    }
}
