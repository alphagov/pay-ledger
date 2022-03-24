package uk.gov.pay.ledger.event.model;

import org.junit.jupiter.api.Test;

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

    @Test
    public void shouldDeriveNonNullServiceIdCorrectlyFromEventDigest() {
        Event eventWithServiceId = anEventFixture()
                .withEventDate(ZonedDateTime.now().minusHours(2L))
                .withServiceId("service-id")
                .toEntity();

        Event eventWithOutServiceId = anEventFixture()
                .withEventDate(ZonedDateTime.now())
                .toEntity();

        Event latestEventWithOutServiceId = anEventFixture()
                .withEventDate(ZonedDateTime.now().plusDays(2))
                .withServiceId(null)
                .toEntity();

        EventDigest eventDigest = EventDigest.fromEventList(
                List.of(eventWithServiceId, eventWithOutServiceId, latestEventWithOutServiceId));

        assertThat(eventDigest.getServiceId(), is("service-id"));
    }

    @Test
    public void shouldDeriveServiceIdToNullIfNoEventsHasServiceId() {
        Event eventWithoutServiceId = anEventFixture()
                .withParentResourceExternalId(null)
                .toEntity();

        Event event2WithoutServiceId = anEventFixture()
                .withParentResourceExternalId(null)
                .toEntity();

        Event event3WithoutServiceId = anEventFixture()
                .withParentResourceExternalId(null)
                .toEntity();

        EventDigest eventDigest = EventDigest.fromEventList(
                List.of(eventWithoutServiceId, event2WithoutServiceId, event3WithoutServiceId));

        assertThat(eventDigest.getParentResourceExternalId(), is(nullValue()));
    }

    @Test
    public void shouldBeLiveTrueIfContainsTrueEvent() {
        Event eventWithNullLiveValue = anEventFixture().withLive(null).toEntity();
        Event eventWithTrueLiveValue = anEventFixture().withLive(true).toEntity();

        EventDigest eventDigest = EventDigest.fromEventList(
                List.of(eventWithTrueLiveValue, eventWithNullLiveValue));

        assertThat(eventDigest.isLive(), is(Boolean.TRUE));
    }

    @Test
    public void shouldBeLiveFalseIfContainsFalseEvent() {
        Event eventWithNullLiveValue = anEventFixture().withLive(null).toEntity();
        Event eventWithFalseLiveValue = anEventFixture().withLive(false).toEntity();

        EventDigest eventDigest = EventDigest.fromEventList(
                List.of(eventWithFalseLiveValue, eventWithNullLiveValue));

        assertThat(eventDigest.isLive(), is(Boolean.FALSE));
    }
    
    @Test
    public void shouldBeLiveNullIfContainsOnlyNullEvents() {
        Event eventWithNullLiveValue = anEventFixture().withLive(null).toEntity();

        EventDigest eventDigest = EventDigest.fromEventList(
                List.of(eventWithNullLiveValue));

        assertThat(eventDigest.isLive(), is(nullValue()));
    }

}