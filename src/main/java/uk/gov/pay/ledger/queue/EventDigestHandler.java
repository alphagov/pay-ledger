package uk.gov.pay.ledger.queue;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.agreement.service.AgreementService;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.payout.service.PayoutService;
import uk.gov.pay.ledger.queue.eventprocessor.AgreementEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.ChildTransactionEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.EventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.PaymentEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.PaymentInstrumentEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.PayoutEventProcessor;
import uk.gov.pay.ledger.transaction.service.TransactionMetadataService;
import uk.gov.pay.ledger.transaction.service.TransactionService;
import uk.gov.pay.ledger.transactionsummary.service.TransactionSummaryService;

import java.time.Clock;

public class EventDigestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDigestHandler.class);

    private PaymentEventProcessor paymentEventProcessor;
    private PayoutEventProcessor payoutEventProcessor;
    private ChildTransactionEventProcessor childTransactionEventProcessor;
    private AgreementEventProcessor agreementEventProcessor;
    private PaymentInstrumentEventProcessor paymentInstrumentEventProcessor;

    @Inject
    public EventDigestHandler(EventService eventService,
                              TransactionService transactionService,
                              TransactionMetadataService transactionMetadataService,
                              PayoutService payoutService,
                              TransactionEntityFactory transactionEntityFactory,
                              TransactionSummaryService transactionSummaryService,
                              AgreementService agreementService,
                              LedgerConfig ledgerConfig,
                              Clock clock) {
        childTransactionEventProcessor = new ChildTransactionEventProcessor(eventService, transactionService, transactionEntityFactory, ledgerConfig, clock);
        paymentEventProcessor = new PaymentEventProcessor(eventService, transactionService, transactionMetadataService, childTransactionEventProcessor, transactionSummaryService);
        payoutEventProcessor = new PayoutEventProcessor(eventService, payoutService);
        agreementEventProcessor = new AgreementEventProcessor(eventService, agreementService);
        paymentInstrumentEventProcessor = new PaymentInstrumentEventProcessor(eventService, agreementService);
    }

    public EventProcessor processorFor(Event event) {
        switch (event.getResourceType()) {
            case PAYMENT:
                return paymentEventProcessor;
            case REFUND:
            case DISPUTE:
                return childTransactionEventProcessor;
            case PAYOUT:
                return payoutEventProcessor;
            case AGREEMENT:
                return agreementEventProcessor;
            case PAYMENT_INSTRUMENT:
                return paymentInstrumentEventProcessor;
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
