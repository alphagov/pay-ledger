package uk.gov.pay.ledger.queue.eventprocessor;

import com.google.inject.Inject;
import uk.gov.pay.ledger.agreement.service.AgreementService;
import uk.gov.pay.ledger.event.dao.entity.EventEntity;
import uk.gov.pay.ledger.event.service.EventService;

public class AgreementEventProcessor extends EventProcessor {
    private final EventService eventService;
    private final AgreementService agreementService;

    @Inject
    public AgreementEventProcessor(EventService eventService, AgreementService agreementService) {
        this.eventService = eventService;
        this.agreementService = agreementService;
    }

    @Override
    public void process(EventEntity event, boolean isNewEvent) {
        agreementService.upsertAgreementFor(eventService.getEventDigestForResource(event));
    }
}
