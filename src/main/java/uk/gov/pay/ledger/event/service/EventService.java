package uk.gov.pay.ledger.event.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.service.model.response.CreateEventResponse;

import java.util.Optional;

public class EventService {

    private EventDao eventDao;

    @Inject
    public EventService(EventDao eventDao) {
        this.eventDao = eventDao;
    }

    public CreateEventResponse createIfDoesNotExist(Event event) {
        try {
            Optional<Long> status = eventDao.insertEventIfDoesNotExistWithResourceTypeId(event);
            return new CreateEventResponse(status);
        } catch (Exception e) {
            return new CreateEventResponse(e);
        }
    }
}

