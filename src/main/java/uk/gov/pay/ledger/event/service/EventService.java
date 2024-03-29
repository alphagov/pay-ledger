package uk.gov.pay.ledger.event.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.entity.EventEntity;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.event.model.response.CreateEventResponse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventService {
    private EventDao eventDao;

    @Inject
    public EventService(EventDao eventDao) {
        this.eventDao = eventDao;
    }

    public EventDigest getEventDigestForResourceAndType(String resourceExternalId, ResourceType resourceType) {
        List<EventEntity> events = getEventsForResource(resourceExternalId)
                .stream().filter(eventEntity -> resourceType == eventEntity.getResourceType())
                .collect(Collectors.toList());
        return EventDigest.fromEventList(events);
    }

    public EventDigest getEventDigestForResource(String resourceExternalId) {
        List<EventEntity> events = getEventsForResource(resourceExternalId);
        return EventDigest.fromEventList(events);
    }

    public List<EventEntity> getEventsForResource(String resourceExternalId) {
        return eventDao.getEventsByResourceExternalId(resourceExternalId);
    }

    public CreateEventResponse createIfDoesNotExist(EventEntity event) {
        try {
            Optional<Long> status = eventDao.insertEventIfDoesNotExistWithResourceTypeId(event);
            return new CreateEventResponse(status);
        } catch (Exception e) {
            return new CreateEventResponse(e);
        }
    }

    public EventDigest getEventDigestForResource(EventEntity event) {
        return getEventDigestForResource(event.getResourceExternalId());
    }
}
