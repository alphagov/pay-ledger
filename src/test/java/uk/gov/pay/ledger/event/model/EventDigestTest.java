package uk.gov.pay.ledger.event.model;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static uk.gov.pay.ledger.util.fixture.QueueEventFixture.aQueueEventFixture;

public class EventDigestTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void fromEventList_ShouldRejectEventDigestWithoutAnySalientEvents() {
        Event firstNonSalientEvent = aQueueEventFixture()
                .withEventType("FIRST_NON_STATE_TRANSITION_EVENT")
                .withResourceType(ResourceType.PAYMENT)
                .toEntity();

        Event secondNonSalientEvent = aQueueEventFixture()
                .withEventType("SECOND_NON_STATE_TRANSITION_EVENT")
                .withResourceType(ResourceType.PAYMENT)
                .toEntity();

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("No supported external state transition events found for digest");
        EventDigest.fromEventList(List.of(firstNonSalientEvent, secondNonSalientEvent));
    }
}