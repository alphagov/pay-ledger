package uk.gov.pay.ledger.event.model;

import java.util.Arrays;
import java.util.Optional;

public enum SalientEventType {
    PAYMENT_CREATED,
    PAYMENT_DETAILS_EVENT;

    public static Optional<SalientEventType> from(String eventName) {
        return Arrays.stream(SalientEventType.values())
                .filter(v -> v.name().equals(eventName))
                .findFirst();
    }
}
