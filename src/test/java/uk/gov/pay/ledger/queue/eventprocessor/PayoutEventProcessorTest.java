package uk.gov.pay.ledger.queue.eventprocessor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.payout.service.PayoutService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.event.model.ResourceType.PAYOUT;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;

@ExtendWith(MockitoExtension.class)
class PayoutEventProcessorTest {

    @Mock
    EventService mockEventService;

    @Mock
    PayoutService mockPayoutService;

    @InjectMocks
    PayoutEventProcessor payoutEventProcessor;

    @Test
    void shouldUpsertPayout() {
        Event event = anEventFixture().withResourceType(PAYOUT).toEntity();
        var eventDigest = EventDigest.fromEventList(List.of(anEventFixture().toEntity()));
        when(mockEventService.getEventDigestForResource(event)).thenReturn(eventDigest);

        payoutEventProcessor.process(event, true);

        verify(mockEventService).getEventDigestForResource(event);
        verify(mockPayoutService).upsertPayoutFor(eventDigest);
    }
}