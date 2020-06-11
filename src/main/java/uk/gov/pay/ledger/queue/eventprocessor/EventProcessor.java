package uk.gov.pay.ledger.queue.eventprocessor;

import uk.gov.pay.ledger.event.model.Event;

public abstract class EventProcessor {
    public abstract void process(Event event);
}
