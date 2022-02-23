package uk.gov.pay.ledger.queue;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.payout.service.PayoutService;
import uk.gov.pay.ledger.queue.eventprocessor.DisputeEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.EventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.PaymentEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.PayoutEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.RefundEventProcessor;
import uk.gov.pay.ledger.transaction.service.TransactionMetadataService;
import uk.gov.pay.ledger.transaction.service.TransactionService;
import uk.gov.pay.ledger.transactionsummary.service.TransactionSummaryService;

public class EventDigestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDigestHandler.class);

    private PaymentEventProcessor paymentEventProcessor;
    private PayoutEventProcessor payoutEventProcessor;
    private RefundEventProcessor refundEventProcessor;
    private DisputeEventProcessor disputeEventProcessor;

    @Inject
    public EventDigestHandler(EventService eventService,
                              TransactionService transactionService,
                              TransactionMetadataService transactionMetadataService,
                              PayoutService payoutService,
                              TransactionEntityFactory transactionEntityFactory, TransactionSummaryService transactionSummaryService) {
        refundEventProcessor = new RefundEventProcessor(eventService, transactionService, transactionEntityFactory);
        paymentEventProcessor = new PaymentEventProcessor(eventService, transactionService, transactionMetadataService, refundEventProcessor, transactionSummaryService);
        payoutEventProcessor = new PayoutEventProcessor(eventService, payoutService);
        disputeEventProcessor = new DisputeEventProcessor();
    }

    public EventProcessor processorFor(Event event) {
        switch (event.getResourceType()) {
            case PAYMENT:
                return paymentEventProcessor;
            case REFUND:
                return refundEventProcessor;
            case PAYOUT:
                return payoutEventProcessor;
            case DISPUTE:
                return disputeEventProcessor;
            default:
                String message = String.format("Event digest processing for resource type [%s] is not supported. Event type [%s] and resource external id [%s]",
                        event.getResourceType(),
                        event.getEventType(),
                        event.getResourceExternalId());
                LOGGER.error(message);
                throw new RuntimeException(message);
        }
    }

    public void processEvent(Event event, boolean isANewEvent) {
        processorFor(event).process(event, isANewEvent);
    }
}
