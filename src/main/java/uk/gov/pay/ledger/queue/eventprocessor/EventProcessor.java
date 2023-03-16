package uk.gov.pay.ledger.queue.eventprocessor;

import uk.gov.pay.ledger.event.model.EventEntity;

public abstract class EventProcessor {
    public abstract void process(EventEntity event, boolean isANewEvent);
}
