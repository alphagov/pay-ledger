package uk.gov.pay.ledger.queue;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.payout.service.PayoutService;
import uk.gov.pay.ledger.transaction.service.TransactionMetadataService;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import static uk.gov.pay.ledger.event.model.ResourceType.PAYMENT;
import static uk.gov.pay.ledger.event.model.ResourceType.PAYOUT;
import static uk.gov.pay.ledger.event.model.ResourceType.REFUND;

public class EventDigestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDigestHandler.class);

    private final EventService eventService;
    private final TransactionService transactionService;
    private final TransactionMetadataService transactionMetadataService;
    private final PayoutService payoutService;

    @Inject
    public EventDigestHandler(EventService eventService,
                              TransactionService transactionService,
                              TransactionMetadataService transactionMetadataService,
                              PayoutService payoutService) {
        this.eventService = eventService;
        this.transactionService = transactionService;
        this.transactionMetadataService = transactionMetadataService;
        this.payoutService = payoutService;
    }

    public void processEvent(Event event) {
        EventDigest eventDigest = eventService.getEventDigestForResource(event.getResourceExternalId());

        if (isATransactionEvent(event.getResourceType())) {
            processDigestForTransactionEvent(event, eventDigest);
        } else if (isAPayoutEvent(event.getResourceType())) {
            processDigestForPayout(eventDigest);
        } else {
            LOGGER.warn("Event digest processing for resource type [{}] is not supported. Event type [{}] and resource external id [{}]",
                    event.getResourceType(),
                    event.getEventType(),
                    event.getResourceExternalId());
        }
    }

    private boolean isAPayoutEvent(ResourceType resourceType) {
        return PAYOUT.equals(resourceType);
    }

    private boolean isATransactionEvent(ResourceType resourceType) {
        return PAYMENT.equals(resourceType) || REFUND.equals(resourceType);
    }

    private void processDigestForTransactionEvent(Event event, EventDigest eventDigest) {
        transactionService.upsertTransactionFor(eventDigest);
        transactionMetadataService.upsertMetadataFor(event);
    }

    private void processDigestForPayout(EventDigest eventDigest) {
        payoutService.upsertPayoutFor(eventDigest);
    }
}
