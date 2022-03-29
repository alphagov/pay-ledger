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

    public EventDigest getEventDigestForResource(String resourceExternalId) {
        List<Event> events = getEventsForResource(resourceExternalId);
        return EventDigest.fromEventList(events);
    }

    public List<Event> getEventsForResource(String resourceExternalId) {
        return eventDao.getEventsByResourceExternalId(resourceExternalId);
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

    public EventDigest getEventDigestForResourceId(String resourceExternalId) {
        return getEventDigestForResource(resourceExternalId);
    }

    public Integer countByResourceExternalId(String resourceExternalId) {
        return eventDao.countById(resourceExternalId);
    }
}