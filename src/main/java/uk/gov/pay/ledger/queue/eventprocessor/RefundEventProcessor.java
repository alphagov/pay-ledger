package uk.gov.pay.ledger.queue.eventprocessor;

import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
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
        transactionService.upsertTransactionFor(refundEventDigest);
    }
}
