package uk.gov.pay.ledger.event.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.response.CreateEventResponse;

import java.util.List;
import java.util.Optional;

public class EventService {
    private EventDao eventDao;

    @Inject
    public EventService(EventDao eventDao) {
        this.eventDao = eventDao;
    }

    private EventDigest getEventDigestForResource(String resourceExternalId) {
        List<Event> events = eventDao.getEventsByResourceExternalId(resourceExternalId);
        return EventDigest.fromEventList(events);
    }

    public CreateEventResponse createIfDoesNotExist(Event event) {
        try {
            Optional<Long> status = eventDao.insertEventIfDoesNotExistWithResourceTypeId(event);
            return new CreateEventResponse(status);
        } catch (Exception e) {
            return new CreateEventResponse(e);
        }
    }

    public EventDigest getEventDigestForResource(Event event) {
        return getEventDigestForResource(event.getResourceExternalId());
    }
}
