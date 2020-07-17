package uk.gov.pay.ledger.queue.eventprocessor;

import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.transaction.service.TransactionMetadataService;
import uk.gov.pay.ledger.transaction.service.TransactionService;

public class PaymentEventProcessor extends EventProcessor {
    private EventService eventService;
    private TransactionService transactionService;
    private TransactionMetadataService transactionMetadataService;

    public PaymentEventProcessor(EventService eventService, TransactionService transactionService,
                                 TransactionMetadataService transactionMetadataService) {

        this.eventService = eventService;
        this.transactionService = transactionService;
        this.transactionMetadataService = transactionMetadataService;
    }

    @Override
    public void process(Event event) {
        EventDigest eventDigest = eventService.getEventDigestForResource(event);
        transactionService.upsertTransactionFor(eventDigest);
        transactionMetadataService.upsertMetadataFor(event);
    }
}
