package uk.gov.pay.ledger.agreement.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.service.payments.commons.model.agreement.AgreementStatus;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

class AgreementsFactoryTest {
    private final AgreementsFactory agreementEntityFactory = new AgreementsFactory(new ObjectMapper());
    private final GsonBuilder gsonBuilder = new GsonBuilder();

    @Test
    public void shouldConvertEventDigestToAgreementEntity() {
        var event = aQueuePaymentEventFixture()
                .withEventType("AGREEMENT_CREATED")
                .withResourceType(ResourceType.AGREEMENT)
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "reference", "agreement-reference",
                                "description", "agreement description text",
                                "user_identifier", "a-valid-user-identifier"
                        ))
                )
                .toEntity();
        var eventDigest = EventDigest.fromEventList(List.of(event));
        var entity = agreementEntityFactory.create(eventDigest);

        assertThat(entity.getServiceId(), is(event.getServiceId()));
        assertThat(entity.getLive(), is(entity.getLive()));
        assertThat(entity.getCreatedDate(), is(event.getEventDate()));
        assertThat(entity.getEventCount(), is(1));
        assertThat(entity.getReference(), is("agreement-reference"));
        assertThat(entity.getDescription(), is("agreement description text"));
        assertThat(entity.getUserIdentifier(), is("a-valid-user-identifier"));
        assertThat(entity.getStatus(), is(AgreementStatus.CREATED));
    }

    @Test
    public void shouldConvertEventDigestToPaymentInstrumentEntity() {
        var event = aQueuePaymentEventFixture()
                .withEventType("PAYMENT_INSTRUMENT_CREATED")
                .withResourceExternalId("ext-id")
                .withResourceType(ResourceType.PAYMENT_INSTRUMENT)
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "agreement_external_id", "agreement-ext-id",
                                "cardholder_name", "Joe D",
                                "address_line1", "An address line 1"
                        ))
                )
                .toEntity();
        var eventDigest = EventDigest.fromEventList(List.of(event));
        var entity = agreementEntityFactory.createPaymentInstrument(eventDigest);

        assertThat(entity.getCreatedDate(), is(event.getEventDate()));
        assertThat(entity.getEventCount(), is(1));
        assertThat(entity.getExternalId(), is("ext-id"));
        assertThat(entity.getAgreementExternalId(), is("agreement-ext-id"));
        assertThat(entity.getCardholderName(), is("Joe D"));
        assertThat(entity.getAddressLine1(), is("An address line 1"));
    }

    @Test
    public void shouldConvertAgreementInactivatedEventDigestToAgreementEntity() {
        var event = aQueuePaymentEventFixture()
                .withEventType("AGREEMENT_INACTIVATED")
                .withResourceType(ResourceType.AGREEMENT)
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "reason", "expired_card",
                                "payment_instrument_external_id", "a-valid-payment-instrument-external-id"
                        ))
                )
                .toEntity();
        var eventDigest = EventDigest.fromEventList(List.of(event));
        var entity = agreementEntityFactory.create(eventDigest);

        assertThat(entity.getServiceId(), is(event.getServiceId()));
        assertThat(entity.getLive(), is(entity.getLive()));
        assertThat(entity.getCreatedDate(), is(event.getEventDate()));
        assertThat(entity.getEventCount(), is(1));
        assertThat(entity.getStatus(), is(AgreementStatus.INACTIVE));
    }

    @Test
    public void shouldConvertCancelledEventDigestToAgreementEntity() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        var event = aQueuePaymentEventFixture()
                .withEventType("AGREEMENT_CANCELLED_BY_USER")
                .withResourceType(ResourceType.AGREEMENT)
                .withEventDate(now)
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "reference", "agreement-reference",
                                "description", "agreement description text",
                                "user_identifier", "a-valid-user-identifier",
                                "user_email", "jdoe@example.org",
                                "cancelled_date", now.toString()
                        ))
                )
                .toEntity();
        var eventDigest = EventDigest.fromEventList(List.of(event));
        var entity = agreementEntityFactory.create(eventDigest);

        assertThat(entity.getServiceId(), is(event.getServiceId()));
        assertThat(entity.getLive(), is(entity.getLive()));
        assertThat(entity.getCreatedDate(), is(event.getEventDate()));
        assertThat(entity.getEventCount(), is(1));
        assertThat(entity.getReference(), is("agreement-reference"));
        assertThat(entity.getDescription(), is("agreement description text"));
        assertThat(entity.getUserIdentifier(), is("a-valid-user-identifier"));
        assertThat(entity.getStatus(), is(AgreementStatus.CANCELLED));
        assertThat(entity.getCancelledByUserEmail(), is("jdoe@example.org"));
        assertThat(entity.getCancelledDate(), is(now));
    }

    @Test
    public void shouldNotReturnUSerEmailIfNewerEventOverWritesIt() {
        var now = ZonedDateTime.now(ZoneOffset.UTC).minusHours(2L);
        var secondNow = ZonedDateTime.now(ZoneOffset.UTC);
        var event1 = aQueuePaymentEventFixture()
                .withEventType("AGREEMENT_CANCELLED_BY_USER")
                .withResourceType(ResourceType.AGREEMENT)
                .withEventDate(now)
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "reference", "agreement-reference",
                                "description", "agreement description text",
                                "user_identifier", "a-valid-user-identifier",
                                "user_email", "jdoe@example.org",
                                "cancelled_date", now.toString()
                        ))
                )
                .toEntity();
        var event2 = aQueuePaymentEventFixture()
                .withEventType("AGREEMENT_CANCELLED_BY_SERVICE")
                .withResourceType(ResourceType.AGREEMENT)
                .withEventDate(secondNow)
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "reference", "agreement-reference",
                                "description", "agreement description text",
                                "user_identifier", "a-valid-user-identifier",
                                "cancelled_date", secondNow.toString()
                        ))
                )
                .toEntity();
        var eventDigest = EventDigest.fromEventList(List.of(event2, event1));
        var entity = agreementEntityFactory.create(eventDigest);

        assertThat(entity.getServiceId(), is(event1.getServiceId()));
        assertThat(entity.getLive(), is(entity.getLive()));
        assertThat(entity.getCreatedDate(), is(event1.getEventDate()));
        assertThat(entity.getEventCount(), is(2));
        assertThat(entity.getReference(), is("agreement-reference"));
        assertThat(entity.getDescription(), is("agreement description text"));
        assertThat(entity.getUserIdentifier(), is("a-valid-user-identifier"));
        assertThat(entity.getStatus(), is(AgreementStatus.CANCELLED));
        assertThat(entity.getCancelledByUserEmail(), is(nullValue()));
        assertThat(entity.getCancelledDate(), is(secondNow));
    }

    @Test
    public void shouldOverwriteLatestCancelledStateByEventDigestWhenThereIsANewerEvent() {
        var now = ZonedDateTime.now(ZoneOffset.UTC).minusHours(2L);
        var secondNow = ZonedDateTime.now(ZoneOffset.UTC).minusHours(1L);
        var thirdNow = ZonedDateTime.now(ZoneOffset.UTC);
        var event1 = aQueuePaymentEventFixture()
                .withEventType("AGREEMENT_CANCELLED_BY_USER")
                .withResourceType(ResourceType.AGREEMENT)
                .withEventDate(now)
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "reference", "agreement-reference",
                                "description", "agreement description text",
                                "user_identifier", "a-valid-user-identifier",
                                "user_email", "jdoe@example.org",
                                "cancelled_date", now.toString()
                        ))
                )
                .toEntity();
        var event2 = aQueuePaymentEventFixture()
                .withEventType("AGREEMENT_CANCELLED_BY_SERVICE")
                .withResourceType(ResourceType.AGREEMENT)
                .withEventDate(secondNow)
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "reference", "agreement-reference",
                                "description", "agreement description text",
                                "user_identifier", "a-valid-user-identifier",
                                "cancelled_date", secondNow.toString()
                        ))
                )
                .toEntity();
        var event3 = aQueuePaymentEventFixture()
                .withEventType("AGREEMENT_CREATED")
                .withResourceType(ResourceType.AGREEMENT)
                .withEventDate(thirdNow)
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "reference", "agreement-reference",
                                "description", "agreement description text",
                                "user_identifier", "a-valid-user-identifier"
                        ))
                )
                .toEntity();
        var eventDigest = EventDigest.fromEventList(List.of(event3, event2, event1));
        var entity = agreementEntityFactory.create(eventDigest);

        assertThat(entity.getServiceId(), is(event1.getServiceId()));
        assertThat(entity.getLive(), is(entity.getLive()));
        assertThat(entity.getCreatedDate(), is(event1.getEventDate()));
        assertThat(entity.getEventCount(), is(3));
        assertThat(entity.getReference(), is("agreement-reference"));
        assertThat(entity.getDescription(), is("agreement description text"));
        assertThat(entity.getUserIdentifier(), is("a-valid-user-identifier"));
        assertThat(entity.getStatus(), is(AgreementStatus.CREATED));
        assertThat(entity.getCancelledByUserEmail(), is(nullValue()));
        assertThat(entity.getCancelledDate(), is(nullValue()));
    }
}
