package uk.gov.pay.ledger.event.services;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.queue.EventMessageDto;

public class EventService {

    private EventDao eventDao;

    @Inject
    public EventService(EventDao eventDao) {
        this.eventDao = eventDao;
    }

    public CreateEventResponse createIfDoesNotExist(EventMessageDto event) {
        return new CreateEventResponse();
    }

    public class CreateEventResponse {
        public boolean isSuccessful() {
            return false;
        }

        public String getState() {
            return "";
        }
    }
}
