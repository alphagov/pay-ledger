package uk.gov.pay.ledger.queue.eventprocessor;

import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.exception.EmptyEventsException;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class RefundEventProcessor extends EventProcessor {
    private final EventService eventService;
    private final TransactionService transactionService;
    private final TransactionEntityFactory transactionEntityFactory;

    public RefundEventProcessor(EventService eventService, TransactionService transactionService,
                                TransactionEntityFactory transactionEntityFactory) {

        this.eventService = eventService;
        this.transactionService = transactionService;
        this.transactionEntityFactory = transactionEntityFactory;
    }

    @Override
    public void process(Event event) {
        EventDigest refundEventDigest = eventService.getEventDigestForResource(event);
        Optional<EventDigest> mayBePaymentEventDigest = Optional.empty();

        if (isNotBlank(refundEventDigest.getParentResourceExternalId())) {
            mayBePaymentEventDigest = getPaymentEventDigest(refundEventDigest.getParentResourceExternalId());
        }

        mayBePaymentEventDigest.ifPresentOrElse(
                paymentEventDigest -> transactionService.upsertRefundTransactionWithPaymentInfo(refundEventDigest, paymentEventDigest),
                () -> transactionService.upsertTransactionFor(refundEventDigest));
    }

    private Optional<EventDigest> getPaymentEventDigest(String paymentExternalId) {
        EventDigest paymentEventDigest = null;
        try {
            paymentEventDigest = eventService.getEventDigestForResource(paymentExternalId);
        } catch (EmptyEventsException ignored) {
            // no valid refund projection is possible without payment events, allow upstream to handle this
        }
        return Optional.ofNullable(paymentEventDigest);
    }
}
