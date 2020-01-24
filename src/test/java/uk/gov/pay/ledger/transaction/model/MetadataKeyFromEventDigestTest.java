package uk.gov.pay.ledger.transaction.model;


import org.junit.Test;
import uk.gov.pay.commons.model.Source;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.event.model.SalientEventType;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

public class MetadataKeyFromEventDigestTest {

    @Test
    public void shouldReturnAMetadataKey() {
        Event paymentCreatedEvent = aQueuePaymentEventFixture()
                .withEventType(SalientEventType.PAYMENT_CREATED.name())
                .withResourceType(ResourceType.PAYMENT)
                .withSource(Source.CARD_API)
                .withMetadata("meta1", "data1")
                .withMetadata("meta2", "data2")
                .withDefaultEventDataForEventType(SalientEventType.PAYMENT_CREATED.name())
                .toEntity();
        EventDigest paymentCreatedEventDigest = EventDigest.fromEventList(List.of(paymentCreatedEvent));
        MetadataKey metadataKey = MetadataKey.from(paymentCreatedEventDigest);
        assertThat(metadataKey.getMetadata().size(), is(2));
        assertThat(metadataKey.getExternalId(), is(paymentCreatedEvent.getResourceExternalId()));
    }

    @Test
    public void shouldReturnEmptyMap() {
        Event paymentCreatedEvent = aQueuePaymentEventFixture()
                .withEventType(SalientEventType.PAYMENT_CREATED.name())
                .withResourceType(ResourceType.PAYMENT)
                .withSource(Source.CARD_API)
                .includeMetadata(false)
                .withDefaultEventDataForEventType(SalientEventType.PAYMENT_CREATED.name())
                .toEntity();
        EventDigest paymentCreatedEventDigest = EventDigest.fromEventList(List.of(paymentCreatedEvent));
        MetadataKey metadataKey = MetadataKey.from(paymentCreatedEventDigest);
        assertThat(metadataKey.getMetadata().size(), is(0));
        assertThat(metadataKey.getExternalId(), is(paymentCreatedEvent.getResourceExternalId()));
    }
}
