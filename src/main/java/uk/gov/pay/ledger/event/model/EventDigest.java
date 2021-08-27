package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.pay.ledger.exception.EmptyEventsException;
import uk.gov.pay.ledger.util.JsonParser;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class EventDigest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ZonedDateTime mostRecentEventTimestamp;
    private final ResourceType resourceType;
    private final String resourceExternalId;
    private final String parentResourceExternalId;
    private final SalientEventType mostRecentSalientEventType;
    private Integer eventCount;
    private Map<String, Object> eventPayload;
    private final ZonedDateTime eventCreatedDate;

    private EventDigest(
            ZonedDateTime mostRecentEventTimestamp,
            SalientEventType mostRecentSalientEventType,
            ResourceType resourceType,
            String resourceExternalId,
            String parentResourceExternalId,
            Integer eventCount,
            Map<String, Object> eventPayload,
            ZonedDateTime eventCreatedDate
    ) {
        this.mostRecentEventTimestamp = mostRecentEventTimestamp;
        this.mostRecentSalientEventType = mostRecentSalientEventType;
        this.resourceType = resourceType;
        this.resourceExternalId = resourceExternalId;
        this.parentResourceExternalId = parentResourceExternalId;
        this.eventCount = eventCount;
        this.eventPayload = eventPayload;
        this.eventCreatedDate = eventCreatedDate;
    }

    public static EventDigest fromEventList(List<Event> events) {
        var eventPayload = buildEventPayload(events);

        var latestEvent = events.stream()
                .findFirst()
                .orElseThrow(() -> new EmptyEventsException("No events found"));

        var latestSalientEventType = events.stream()
                .map(Event::getEventType)
                .map(SalientEventType::from)
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(null);

        var earliestDate = events.stream()
                .map(Event::getEventDate)
                .min(ZonedDateTime::compareTo)
                .orElseThrow();

        String parentResourceExternalId = deriveParentResourceExternalId(events);

        return new EventDigest(
                latestEvent.getEventDate(),
                latestSalientEventType,
                latestEvent.getResourceType(),
                latestEvent.getResourceExternalId(),
                parentResourceExternalId,
                events.size(),
                eventPayload,
                earliestDate
        );
    }

    private static String deriveParentResourceExternalId(List<Event> events) {
        return events.stream()
                .filter(event -> isNotEmpty(event.getParentResourceExternalId()))
                .map(Event::getParentResourceExternalId)
                .findFirst()
                .orElse(null);
    }

    private static Map<String, Object> buildEventPayload(List<Event> events) {
        return events.stream()
                .map(Event::getEventData)
                .map(JsonParser::jsonStringToMap)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (later, earlier) -> later));
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

    public String getParentResourceExternalId() {
        return parentResourceExternalId;
    }

    public Optional<SalientEventType> getMostRecentSalientEventType() {
        return Optional.ofNullable(mostRecentSalientEventType);
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
