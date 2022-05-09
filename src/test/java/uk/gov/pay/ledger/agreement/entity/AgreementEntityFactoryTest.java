package uk.gov.pay.ledger.agreement.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.ResourceType;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

class AgreementEntityFactoryTest {
    private AgreementEntityFactory agreementEntityFactory;

    private GsonBuilder gsonBuilder = new GsonBuilder();
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        agreementEntityFactory = new AgreementEntityFactory(objectMapper);
    }

    @Test
    public void shouldConvertEventDigestToAgreementEntity() {
        var event = aQueuePaymentEventFixture()
                .withEventType("AGREEMENT_CREATED")
                .withResourceType(ResourceType.AGREEMENT)
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "reference", "agreement-reference",
                                "description", "agreement description text",
                                "status", "CREATED"
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
        assertThat(entity.getStatus(), is("CREATED"));
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
}