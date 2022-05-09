package uk.gov.pay.ledger.queue.eventprocessor;

import uk.gov.pay.ledger.agreement.service.AgreementService;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.service.EventService;

public class AgreementEventProcessor extends EventProcessor {
    private EventService eventService;
    private AgreementService agreementService;

    public AgreementEventProcessor(EventService eventService, AgreementService agreementService) {
        this.eventService = eventService;
        this.agreementService = agreementService;
    }

    @Override
    public void process(Event event, boolean isANewEvent) {
        agreementService.upsertAgreementFor(eventService.getEventDigestForResource(event));
    }
}