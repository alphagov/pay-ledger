package uk.gov.pay.ledger.queue;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.queue.eventprocessor.AgreementEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.ChildTransactionEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.EventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.PaymentEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.PaymentInstrumentEventProcessor;
import uk.gov.pay.ledger.queue.eventprocessor.PayoutEventProcessor;

public class EventDigestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDigestHandler.class);

    private PaymentEventProcessor paymentEventProcessor;
    private PayoutEventProcessor payoutEventProcessor;
    private ChildTransactionEventProcessor childTransactionEventProcessor;
    private AgreementEventProcessor agreementEventProcessor;
    private PaymentInstrumentEventProcessor paymentInstrumentEventProcessor;

    @Inject
    public EventDigestHandler(PaymentEventProcessor paymentEventProcessor,
                              PayoutEventProcessor payoutEventProcessor,
                              ChildTransactionEventProcessor childTransactionEventProcessor,
                              AgreementEventProcessor agreementEventProcessor,
                              PaymentInstrumentEventProcessor paymentInstrumentEventProcessor) {

        this.paymentEventProcessor = paymentEventProcessor;
        this.payoutEventProcessor = payoutEventProcessor;
        this.childTransactionEventProcessor = childTransactionEventProcessor;
        this.agreementEventProcessor = agreementEventProcessor;
        this.paymentInstrumentEventProcessor = paymentInstrumentEventProcessor;
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
