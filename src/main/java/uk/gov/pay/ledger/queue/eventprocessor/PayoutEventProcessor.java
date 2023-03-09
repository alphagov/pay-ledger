package uk.gov.pay.ledger.queue.eventprocessor;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.dao.entity.EventEntity;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.payout.service.PayoutService;

public class PayoutEventProcessor extends EventProcessor {
    private EventService eventService;
    private PayoutService payoutService;

    @Inject
    public PayoutEventProcessor(EventService eventService, PayoutService payoutService) {
        this.eventService = eventService;
        this.payoutService = payoutService;
    }

    @Override
    public void process(EventEntity event, boolean isANewEvent) {
        payoutService.upsertPayoutFor(eventService.getEventDigestForResource(event));
    }
}
