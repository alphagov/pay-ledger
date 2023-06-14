package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.pay.ledger.event.entity.EventEntity;
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
    private final SalientEventType latestSalientEventType;

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
            ZonedDateTime eventCreatedDate,
            SalientEventType latestSalientEventType) {
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
        this.latestSalientEventType = latestSalientEventType;
    }

    public static EventDigest fromEventList(List<EventEntity> events) {
        var eventPayload = buildEventAggregate(events);

        var latestEvent = events.stream()
                .findFirst()
                .orElseThrow(() -> new EmptyEventsException("No events found"));

        var latestSalientEventType = events.stream()
                .map(EventEntity::getEventType)
                .map(SalientEventType::from)
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(null);

        var earliestDate = events.stream()
                .map(EventEntity::getEventDate)
                .min(ZonedDateTime::compareTo)
                .orElseThrow();

        String parentResourceExternalId = deriveParentResourceExternalId(events);
        String serviceId = deriveServiceId(events);

        Boolean isLive = events.stream()
                .filter(event -> event.getLive() != null)
                .map(EventEntity::getLive)
                .findFirst()
                .orElse(null);

        return new EventDigest(
                serviceId,
                isLive,
                latestEvent.getEventDate(),
                latestSalientEventType,
                latestEvent.getResourceType(),
                latestEvent.getResourceExternalId(),
                parentResourceExternalId,
                events.size(),
                eventPayload,
                earliestDate,
                latestSalientEventType
        );
    }

    private static String deriveServiceId(List<EventEntity> events) {
        return events.stream()
                .filter(event -> isNotEmpty(event.getServiceId()))
                .map(EventEntity::getServiceId)
                .findFirst()
                .orElse(null);
    }

    private static String deriveParentResourceExternalId(List<EventEntity> events) {
        return events.stream()
                .filter(event -> isNotEmpty(event.getParentResourceExternalId()))
                .map(EventEntity::getParentResourceExternalId)
                .findFirst()
                .orElse(null);
    }

    private static Map<String, Object> buildEventAggregate(List<EventEntity> events) {
        return events.stream()
                .map(EventEntity::getEventData)
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

    public Optional<SalientEventType> getLatestSalientEventType() {
        return Optional.ofNullable(latestSalientEventType);
    }
}
