package uk.gov.pay.ledger.queue.eventprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.Event;

public class DisputeEventProcessor extends EventProcessor{

    private static final Logger LOGGER = LoggerFactory.getLogger(DisputeEventProcessor.class);

    @Override
    public void process(Event event, boolean isANewEvent) {
        LOGGER.info("{} resource type processing is not yet implemented. Event type {} and resource external id {}",
                event.getResourceType(),
                event.getEventType(),
                event.getResourceExternalId());
    }
}
