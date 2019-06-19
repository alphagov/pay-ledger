package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventDigest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ZonedDateTime mostRecentEventTimestamp;
    private final EventType mostRecentEventType;
    private final ResourceType resourceType;
    private final String resourceExternalId;
    private final Map<String, Object> eventDetailsDigest;

    private EventDigest(ZonedDateTime mostRecentEventTimestamp, EventType mostRecentEventType, ResourceType resourceType, String resourceExternalId, Map<String, Object> eventDetailsDigest) {
        this.mostRecentEventTimestamp = mostRecentEventTimestamp;
        this.mostRecentEventType = mostRecentEventType;
        this.resourceType = resourceType;
        this.resourceExternalId = resourceExternalId;
        this.eventDetailsDigest = eventDetailsDigest;
    }

    public static EventDigest fromEventList(List<Event> events) {
        Map<String, Object> eventDetailsDigest = getEventDetailsDigest(events);
        return events.stream()
                .findFirst()
                .map(e -> EventDigest.from(e, eventDetailsDigest))
                .orElseThrow(() -> new RuntimeException("No events found"));
    }

    private static EventDigest from(Event latestEvent, Map<String, Object> eventDetailsDigest) {
        return new EventDigest(
                latestEvent.getEventDate(),
                latestEvent.getEventType(),
                latestEvent.getResourceType(),
                latestEvent.getResourceExternalId(),
                eventDetailsDigest
        );
    }

    private static Map<String, Object> getEventDetailsDigest(List<Event> events) {
        return events.stream()
                .map(Event::getEventData)
                .map(EventDigest::eventDetailsJsonStringToMap)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (later, earlier) -> later));
    }

    private static Map<String, Object> eventDetailsJsonStringToMap(String eventDetails) {
        try {
            return (Map<String, Object>) objectMapper.readValue(eventDetails, Map.class);
        } catch (IOException | ClassCastException e) {
            throw new RuntimeException("Error converting event Json to Map");
        }
    }

    public ZonedDateTime getMostRecentEventTimestamp() {
        return mostRecentEventTimestamp;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getResourceExternalId() {
        return resourceExternalId;
    }

    public Map<String, Object> getEventDetailsDigest() {
        return eventDetailsDigest;
    }

    public EventType getMostRecentEventType() {
        return mostRecentEventType;
    }
}
