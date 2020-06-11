package uk.gov.pay.ledger.queue.eventprocessor;

import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.payout.service.PayoutService;

public class PayoutEventProcessor extends EventProcessor {
    private EventService eventService;
    private PayoutService payoutService;

    public PayoutEventProcessor(EventService eventService, PayoutService payoutService) {
        this.eventService = eventService;
        this.payoutService = payoutService;
    }

    @Override
    public void process(Event event) {
        payoutService.upsertPayoutFor(eventService.getEventDigestForResource(event));
    }
}
