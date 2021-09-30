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
    private final String serviceId;
    private final Boolean live;
    private final ZonedDateTime mostRecentEventTimestamp;
    private final ResourceType resourceType;
    private final String resourceExternalId;
    private final String parentResourceExternalId;
    private final SalientEventType mostRecentSalientEventType;
    private Integer eventCount;
    private Map<String, Object> eventAggregate;
    private final ZonedDateTime eventCreatedDate;

    private EventDigest(
            String serviceId,
            Boolean live,
            ZonedDateTime mostRecentEventTimestamp,
            SalientEventType mostRecentSalientEventType,
            ResourceType resourceType,
            String resourceExternalId,
            String parentResourceExternalId,
            Integer eventCount,
            Map<String, Object> eventAggregate,
            ZonedDateTime eventCreatedDate
    ) {
        this.serviceId = serviceId;
        this.live = live;
        this.mostRecentEventTimestamp = mostRecentEventTimestamp;
        this.mostRecentSalientEventType = mostRecentSalientEventType;
        this.resourceType = resourceType;
        this.resourceExternalId = resourceExternalId;
        this.parentResourceExternalId = parentResourceExternalId;
        this.eventCount = eventCount;
        this.eventAggregate = eventAggregate;
        this.eventCreatedDate = eventCreatedDate;
    }

    public static EventDigest fromEventList(List<Event> events) {
        var eventPayload = buildEventAggregate(events);

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

        Boolean isLive = events.stream()
                .filter(event -> event.getLive() != null)
                .map(Event::getLive)
                .findFirst()
                .orElse(null);

        return new EventDigest(
                latestEvent.getServiceId(),
                isLive,
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

    private static Map<String, Object> buildEventAggregate(List<Event> events) {
        return events.stream()
                .map(Event::getEventData)
                .map(JsonParser::jsonStringToMap)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (later, earlier) -> later));
    }

    public String getServiceId() {
        return serviceId;
    }

    public Boolean isLive() {
        return live;
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

    public Map<String, Object> getEventAggregate() {
        return eventAggregate;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public ZonedDateTime getEventCreatedDate() {
        return eventCreatedDate;
    }
}
