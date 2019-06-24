package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.pay.ledger.transaction.model.Transaction;

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
    private final EventDetailsDigest eventDetailsDigest;
    private final SalientEventType mostRecentSalientEventType;
    private Integer eventCount;

    private EventDigest(
            ZonedDateTime mostRecentEventTimestamp,
            SalientEventType mostRecentSalientEventType,
            ResourceType resourceType,
            String resourceExternalId,
            Integer eventCount,
            EventDetailsDigest eventDetailsDigest
    ) {
        this.mostRecentEventTimestamp = mostRecentEventTimestamp;
        this.mostRecentSalientEventType = mostRecentSalientEventType;
        this.resourceType = resourceType;
        this.resourceExternalId = resourceExternalId;
        this.eventCount = eventCount;
        this.eventDetailsDigest = eventDetailsDigest;
    }

    public static EventDigest fromEventList(List<Event> events) {
        var eventDetailsDigest = buildEventDetailsDigest(events);

        var latestEvent = events.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No events found"));

        var latestSalientEventType = events.stream()
                .map(e -> SalientEventType.from(e.getEventType()))
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(SalientEventType.PAYMENT_CREATED);

        return new EventDigest(
                latestEvent.getEventDate(),
                latestSalientEventType,
                latestEvent.getResourceType(),
                latestEvent.getResourceExternalId(),
                events.size(),
                eventDetailsDigest
        );
    }

    private static EventDetailsDigest buildEventDetailsDigest(List<Event> events) {
        Map<String, Object> eventDetailsMap = events.stream()
                .map(Event::getEventData)
                .map(EventDigest::eventDetailsJsonStringToMap)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (later, earlier) -> later));

        return objectMapper.convertValue(eventDetailsMap, EventDetailsDigest.class);
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

    public EventDetailsDigest getEventDetailsDigest() {
        return eventDetailsDigest;
    }

    public Transaction toTransaction() {
        return new Transaction(
                eventDetailsDigest.getGatewayAccountId(),
                eventDetailsDigest.getAmount(),
                eventDetailsDigest.getReference(),
                eventDetailsDigest.getDescription(),
                "Created",
                eventDetailsDigest.getLanguage(),
                getResourceExternalId(),
                eventDetailsDigest.getReturnUrl(),
                eventDetailsDigest.getEmail(),
                eventDetailsDigest.getPaymentProvider(),
                getMostRecentEventTimestamp(),
                null,
                eventDetailsDigest.getDelayedCapture(),
                null
        );
    }
}
