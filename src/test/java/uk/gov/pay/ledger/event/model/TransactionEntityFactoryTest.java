package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.service.payments.commons.model.Source;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

public class TransactionEntityFactoryTest {

    private TransactionEntityFactory transactionEntityFactory;

    private static ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        transactionEntityFactory = new TransactionEntityFactory(new ObjectMapper());
    }

    @Test
    public void fromShouldConvertEventDigestToTransactionEntity() {
        Event paymentCreatedEvent = aQueuePaymentEventFixture()
                .withEventType(SalientEventType.PAYMENT_CREATED.name())
                .withDefaultEventDataForEventType(SalientEventType.PAYMENT_CREATED.name())
                .withResourceType(ResourceType.PAYMENT)
                .withSource(Source.CARD_API)
                .toEntity();
        Event paymentDetailsEvent = aQueuePaymentEventFixture()
                .withEventType("PAYMENT_DETAILS_ENTERED")
                .withDefaultEventDataForEventType("PAYMENT_DETAILS_ENTERED")
                .withResourceType(ResourceType.PAYMENT)
                .toEntity();
        Event captureConfirmedEvent = aQueuePaymentEventFixture()
                .withEventType("CAPTURE_CONFIRMED")
                .withEventData("{\"net_amount\": 55, \"total_amount\": 105, \"fee\": 33}")
                .withResourceType(ResourceType.PAYMENT)
                .toEntity();
        Event paymentIncludedInPayoutEvent = aQueuePaymentEventFixture()
                .withEventType("PAYMENT_INCLUDED_IN_PAYOUT")
                .withEventData("{\"gateway_payout_id\": \"payout-id\"}")
                .withResourceType(ResourceType.PAYMENT)
                .toEntity();
        EventDigest eventDigest = EventDigest.fromEventList(List.of(paymentCreatedEvent, paymentDetailsEvent,
                captureConfirmedEvent, paymentIncludedInPayoutEvent));

        TransactionEntity transactionEntity = transactionEntityFactory.create(eventDigest);

        assertThat(transactionEntity.getExternalId(), is(eventDigest.getResourceExternalId()));
        assertThat(transactionEntity.getState().toString(), is("CREATED"));
        assertThat(transactionEntity.getCreatedDate(), is(paymentCreatedEvent.getEventDate()));
        assertThat(transactionEntity.getEventCount(), is(eventDigest.getEventCount()));
        assertThat(transactionEntity.getReference(), is(eventDigest.getEventPayload().get("reference")));
        assertThat(transactionEntity.getGatewayAccountId(), is(eventDigest.getEventPayload().get("gateway_account_id")));
        assertThat(transactionEntity.getCardholderName(), is(eventDigest.getEventPayload().get("cardholder_name")));
        assertThat(transactionEntity.getEmail(), is(eventDigest.getEventPayload().get("email")));
        assertThat(transactionEntity.getCardBrand(), is(eventDigest.getEventPayload().get("card_brand")));
        assertThat(transactionEntity.getDescription(), is(eventDigest.getEventPayload().get("description")));
        assertThat(transactionEntity.getFirstDigitsCardNumber(), is(eventDigest.getEventPayload().get("first_digits_card_number")));
        assertThat(transactionEntity.getLastDigitsCardNumber(), is(eventDigest.getEventPayload().get("last_digits_card_number")));
        assertThat(transactionEntity.getAmount(), is(((Integer)eventDigest.getEventPayload().get("amount")).longValue()));
        assertThat(transactionEntity.getNetAmount(), is(((Integer)eventDigest.getEventPayload().get("net_amount")).longValue()));
        assertThat(transactionEntity.getTotalAmount(), is(((Integer)eventDigest.getEventPayload().get("total_amount")).longValue()));
        assertThat(transactionEntity.getFee(), is(((Integer)eventDigest.getEventPayload().get("fee")).longValue()));
        assertThat(transactionEntity.getTransactionType(), is("PAYMENT"));
        assertThat(transactionEntity.getSource(), is(notNullValue()));
        assertThat(transactionEntity.isLive(), is(true));
        assertThat(transactionEntity.getGatewayPayoutId(), is("payout-id"));

        JsonObject transactionDetails = JsonParser.parseString(transactionEntity.getTransactionDetails()).getAsJsonObject();
        assertThat(transactionDetails.get("language").getAsString(), is("en"));
        assertThat(transactionDetails.get("payment_provider").getAsString(), is("sandbox"));
        assertThat(transactionDetails.get("expiry_date").getAsString(), is("11/21"));
        assertThat(transactionDetails.get("address_line1").getAsString(), is("12 Rouge Avenue"));
        assertThat(transactionDetails.get("address_postcode").getAsString(), is("N1 3QU"));
        assertThat(transactionDetails.get("address_country").getAsString(), is("GB"));
        assertThat(transactionDetails.get("delayed_capture").getAsBoolean(), is(false));
        assertThat(transactionDetails.get("return_url").getAsString(), is("https://example.org"));
        assertThat(transactionDetails.get("corporate_surcharge").getAsInt(), is(5));
        assertThat(transactionDetails.get("gateway_transaction_id").getAsString(), is(eventDigest.getEventPayload().get("gateway_transaction_id")));
        assertThat(transactionDetails.get("external_metadata").getAsJsonObject().get("key").getAsString(), is("value"));
    }

    @Test
    public void fromShouldConvertEventDigestToTransactionForChildResource() throws IOException {
        String parentResourceExternalId = "parent-resource-external-id";
        Event refundCreatedEvent = aQueuePaymentEventFixture()
                .withEventType("REFUND_CREATED_BY_USER")
                .withResourceExternalId("resource-external-id")
                .withParentResourceExternalId(parentResourceExternalId)
                .withResourceType(ResourceType.REFUND)
                .withEventData("{\"refunded_by\": \"refunded-by-id\", \"amount\": 1000}")
                .toEntity();
        Event refundSubmittedEvent = aQueuePaymentEventFixture()
                .withEventType("REFUND_SUBMITTED")
                .withResourceExternalId("resource-external-id")
                .withParentResourceExternalId(parentResourceExternalId)
                .withResourceType(ResourceType.REFUND)
                .toEntity();
        EventDigest eventDigest = EventDigest.fromEventList(List.of(refundCreatedEvent, refundSubmittedEvent));

        TransactionEntity transactionEntity = transactionEntityFactory.create(eventDigest);

        assertThat(transactionEntity.getTransactionType(), is(eventDigest.getResourceType().toString()));
        assertThat(transactionEntity.getTransactionType(), is("REFUND"));
        assertThat(transactionEntity.getParentExternalId(), is(eventDigest.getParentResourceExternalId()));
        assertThat(transactionEntity.getParentExternalId(), is(parentResourceExternalId));
        assertThat(transactionEntity.getState().toString(), is("SUBMITTED"));
        assertThat(transactionEntity.getAmount(), is(1000L));

        Map<String, String> transactionDetails = objectMapper.readValue(transactionEntity.getTransactionDetails(), Map.class);
        assertThat(transactionDetails.get("refunded_by"), is("refunded-by-id"));
    }

    @Test
    public void create_ShouldCorrectlySetStateForMostRecentSalientEventType() {
        Event paymentCreatedEvent = aQueuePaymentEventFixture().withEventType("PAYMENT_CREATED").toEntity();
        Event nonSalientEvent = aQueuePaymentEventFixture().withEventType("NON_STATE_TRANSITION_EVENT").toEntity();
        Event paymentStartedEvent = aQueuePaymentEventFixture().withEventType("PAYMENT_STARTED").toEntity();
        Event secondNonSalientEvent = aQueuePaymentEventFixture().withEventType("SECOND_NON_STATE_TRANSITION_EVENT").toEntity();

        EventDigest eventDigest = EventDigest.fromEventList(List.of(secondNonSalientEvent, paymentStartedEvent, nonSalientEvent, paymentCreatedEvent));
        TransactionEntity transactionEntity = transactionEntityFactory.create(eventDigest);

        assertThat(transactionEntity.getState().toString(), is("STARTED"));
    }

    @Test
    public void create_ShouldSetUndefinedStateForNoSalientEventTypes() {
        Event paymentDetailsEntered = aQueuePaymentEventFixture().withEventType("PAYMENT_DETAILS_ENTERED").toEntity();

        EventDigest eventDigest = EventDigest.fromEventList(List.of(paymentDetailsEntered));
        TransactionEntity transactionEntity = transactionEntityFactory.create(eventDigest);

        assertThat(transactionEntity.getState().toString(), is("UNDEFINED"));
    }

    @Test
    public void createWithNoSourceParameter_ShouldHandleNullValueCorrectly() {
        Event paymentCreatedEvent = aQueuePaymentEventFixture()
                .withEventType("PAYMENT_CREATED")
                .withEventData("{\"amount\":50}")
                .toEntity();

        EventDigest eventDigest = EventDigest.fromEventList(List.of(paymentCreatedEvent));
        TransactionEntity transactionEntity = transactionEntityFactory.create(eventDigest);

        assertThat(transactionEntity.getSource(), is(nullValue()));
    }

}