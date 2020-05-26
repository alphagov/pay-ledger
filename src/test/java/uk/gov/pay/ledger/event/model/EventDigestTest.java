package uk.gov.pay.ledger.event.model;

import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;

public class EventDigestTest {

    @Test
    public void shouldDeriveNonNullParentExternalIdCorrectlyFromEventDigest() {
        Event eventWithParentExternalId = anEventFixture()
                .withEventDate(ZonedDateTime.now().minusHours(2L))
                .withParentResourceExternalId("parent_external_id")
                .toEntity();

        Event eventWithOutParentExternalId = anEventFixture()
                .withEventDate(ZonedDateTime.now())
                .withParentResourceExternalId(null)
                .toEntity();

        Event latestEventWithOutParentExternalId = anEventFixture()
                .withEventDate(ZonedDateTime.now().plusDays(2))
                .withParentResourceExternalId(null)
                .toEntity();

        EventDigest eventDigest = EventDigest.fromEventList(
                List.of(latestEventWithOutParentExternalId, eventWithOutParentExternalId, eventWithParentExternalId));

        assertThat(eventDigest.getParentResourceExternalId(), is("parent_external_id"));
    }

    @Test
    public void shouldDeriveParentExternalIdToNullIfNoEventsHasParentExternalId() {
        Event eventWithoutParentExternalId = anEventFixture()
                .withParentResourceExternalId(null)
                .toEntity();

        Event event2WithoutParentExternalId = anEventFixture()
                .withParentResourceExternalId(null)
                .toEntity();

        Event event3WithoutParentExternalId = anEventFixture()
                .withParentResourceExternalId(null)
                .toEntity();

        EventDigest eventDigest = EventDigest.fromEventList(
                List.of(eventWithoutParentExternalId, event2WithoutParentExternalId, event3WithoutParentExternalId));

        assertThat(eventDigest.getParentResourceExternalId(), is(nullValue()));
    }
}