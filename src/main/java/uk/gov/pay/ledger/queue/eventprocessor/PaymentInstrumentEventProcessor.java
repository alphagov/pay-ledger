package uk.gov.pay.ledger.queue.eventprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.agreement.service.AgreementService;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.service.EventService;

public class PaymentInstrumentEventProcessor extends EventProcessor {
    private EventService eventService;
    private AgreementService agreementService;
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentInstrumentEventProcessor.class);


    public PaymentInstrumentEventProcessor(EventService eventService, AgreementService agreementService) {
        this.eventService = eventService;
        this.agreementService = agreementService;
    }

    @Override
    public void process(Event event, boolean isANewEvent) {
        agreementService.upsertPaymentInstrumentFor(eventService.getEventDigestForResource(event));
    }
}