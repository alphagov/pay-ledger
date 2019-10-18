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
        List<Event> events = eventDao.getEventsByResourceExternalId(resourceExternalId);
        return EventDigest.fromEventList(events);
    }

    private CreateEventResponse updateExistingEvent(Event event) {
        try {
            return new CreateEventResponse(eventDao.updateIfExistsWithResourceTypeId(event));
        } catch (Exception e) {
            return new CreateEventResponse(e);
        }
    }

    public CreateEventResponse createOrUpdateIfExists(Event event) {
        try {
            Optional<Long> status = eventDao.insertEventIfDoesNotExistWithResourceTypeId(event);
            return status.isPresent() ? new CreateEventResponse(status) : updateExistingEvent(event);
        } catch (Exception e) {
            return new CreateEventResponse(e);
        }
    }
}
