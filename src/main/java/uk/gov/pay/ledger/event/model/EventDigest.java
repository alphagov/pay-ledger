package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventDigest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ZonedDateTime mostRecentEventTimestamp;
    private final ResourceType resourceType;
    private final String resourceExternalId;
    private final SalientEventType mostRecentSalientEventType;
    private Integer eventCount;
    private Map<String, Object> eventPayload;
    private final ZonedDateTime eventCreatedDate;

    private EventDigest(
            ZonedDateTime mostRecentEventTimestamp,
            SalientEventType mostRecentSalientEventType,
            ResourceType resourceType,
            String resourceExternalId,
            Integer eventCount,
            Map<String, Object> eventPayload,
            ZonedDateTime eventCreatedDate
    ) {
        this.mostRecentEventTimestamp = mostRecentEventTimestamp;
        this.mostRecentSalientEventType = mostRecentSalientEventType;
        this.resourceType = resourceType;
        this.resourceExternalId = resourceExternalId;
        this.eventCount = eventCount;
        this.eventPayload = eventPayload;
        this.eventCreatedDate = eventCreatedDate;
    }

    public static EventDigest fromEventList(List<Event> events) {
        var eventPayload = buildEventPayload(events);

        var latestEvent = events.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No events found"));

        var latestSalientEventType = events.stream()
                .map(e -> SalientEventType.from(e.getEventType()))
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(SalientEventType.PAYMENT_CREATED);

        var earliestDate = events.stream()
                .map(event -> event.getEventDate())
                .min(ZonedDateTime::compareTo).get();

        return new EventDigest(
                latestEvent.getEventDate(),
                latestSalientEventType,
                latestEvent.getResourceType(),
                latestEvent.getResourceExternalId(),
                events.size(),
                eventPayload,
                earliestDate
        );
    }

    private static Map<String, Object> buildEventPayload(List<Event> events) {
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

    public SalientEventType getMostRecentSalientEventType() {
        return mostRecentSalientEventType;
    }

    public Map<String, Object> getEventPayload() {
        return eventPayload;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public ZonedDateTime getEventCreatedDate() {
        return eventCreatedDate;
    }
}
