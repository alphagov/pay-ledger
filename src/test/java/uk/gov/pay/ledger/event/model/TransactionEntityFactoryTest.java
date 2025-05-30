package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.ledger.event.entity.EventEntity;
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
        EventEntity paymentCreatedEvent = aQueuePaymentEventFixture()
                .withLive(true)
                .withEventType(SalientEventType.PAYMENT_CREATED.name())
                .withDefaultEventDataForEventType(SalientEventType.PAYMENT_CREATED.name())
                .withResourceType(ResourceType.PAYMENT)
                .withSource(Source.CARD_API)
                .toEntity();
        EventEntity paymentDetailsEvent = aQueuePaymentEventFixture()
                .withEventType("PAYMENT_DETAILS_ENTERED")
                .withDefaultEventDataForEventType("PAYMENT_DETAILS_ENTERED")
                .withResourceType(ResourceType.PAYMENT)
                .toEntity();
        EventEntity captureConfirmedEvent = aQueuePaymentEventFixture()
                .withEventType("CAPTURE_CONFIRMED")
                .withEventData("{\"net_amount\": 55, \"total_amount\": 105, \"fee\": 33}")
                .withResourceType(ResourceType.PAYMENT)
                .toEntity();
        EventEntity paymentIncludedInPayoutEvent = aQueuePaymentEventFixture()
                .withEventType("PAYMENT_INCLUDED_IN_PAYOUT")
                .withEventData("{\"gateway_payout_id\": \"payout-id\"}")
                .withResourceType(ResourceType.PAYMENT)
                .toEntity();
        EventDigest eventDigest = EventDigest.fromEventList(List.of(paymentCreatedEvent, paymentDetailsEvent,
                captureConfirmedEvent, paymentIncludedInPayoutEvent));

        TransactionEntity transactionEntity = transactionEntityFactory.create(eventDigest);

        assertThat(transactionEntity.getExternalId(), is(eventDigest.getResourceExternalId()));
        assertThat(transactionEntity.getServiceId(), is(paymentCreatedEvent.getServiceId()));
        assertThat(transactionEntity.isLive(), is(paymentCreatedEvent.getLive()));
        assertThat(transactionEntity.getState().toString(), is("CREATED"));
        assertThat(transactionEntity.getCreatedDate(), is(paymentCreatedEvent.getEventDate()));
        assertThat(transactionEntity.getEventCount(), is(eventDigest.getEventCount()));
        assertThat(transactionEntity.getReference(), is(eventDigest.getEventAggregate().get("reference")));
        assertThat(transactionEntity.getGatewayAccountId(), is(eventDigest.getEventAggregate().get("gateway_account_id")));
        assertThat(transactionEntity.getCardholderName(), is(eventDigest.getEventAggregate().get("cardholder_name")));
        assertThat(transactionEntity.getEmail(), is(eventDigest.getEventAggregate().get("email")));
        assertThat(transactionEntity.getCardBrand(), is(eventDigest.getEventAggregate().get("card_brand")));
        assertThat(transactionEntity.getDescription(), is(eventDigest.getEventAggregate().get("description")));
        assertThat(transactionEntity.getFirstDigitsCardNumber(), is(eventDigest.getEventAggregate().get("first_digits_card_number")));
        assertThat(transactionEntity.getLastDigitsCardNumber(), is(eventDigest.getEventAggregate().get("last_digits_card_number")));
        assertThat(transactionEntity.getAmount(), is(((Integer)eventDigest.getEventAggregate().get("amount")).longValue()));
        assertThat(transactionEntity.getNetAmount(), is(((Integer)eventDigest.getEventAggregate().get("net_amount")).longValue()));
        assertThat(transactionEntity.getTotalAmount(), is(((Integer)eventDigest.getEventAggregate().get("total_amount")).longValue()));
        assertThat(transactionEntity.getFee(), is(((Integer)eventDigest.getEventAggregate().get("fee")).longValue()));
        assertThat(transactionEntity.getTransactionType(), is("PAYMENT"));
        assertThat(transactionEntity.getSource(), is(notNullValue()));
        assertThat(transactionEntity.isLive(), is(true));
        assertThat(transactionEntity.getGatewayPayoutId(), is("payout-id"));
        assertThat(transactionEntity.getAgreementId(), is("an-agreement-external-id"));

        JsonObject transactionDetails = JsonParser.parseString(transactionEntity.getTransactionDetails()).getAsJsonObject();
        assertThat(transactionDetails.get("language").getAsString(), is("en"));
        assertThat(transactionDetails.get("payment_provider").getAsString(), is("sandbox"));
        assertThat(transactionDetails.get("expiry_date").getAsString(), is("12/99"));
        assertThat(transactionDetails.get("address_line1").getAsString(), is("125 Kingsway"));
        assertThat(transactionDetails.get("address_postcode").getAsString(), is("WC2B 6NH"));
        assertThat(transactionDetails.get("address_country").getAsString(), is("GB"));
        assertThat(transactionDetails.get("delayed_capture").getAsBoolean(), is(false));
        assertThat(transactionDetails.get("return_url").getAsString(), is("http://return.invalid"));
        assertThat(transactionDetails.get("corporate_surcharge").getAsInt(), is(5));
        assertThat(transactionDetails.get("gateway_transaction_id").getAsString(), is(eventDigest.getEventAggregate().get("gateway_transaction_id")));
        assertThat(transactionDetails.get("external_metadata").getAsJsonObject().get("key").getAsString(), is("value"));
        assertThat(transactionDetails.get("canRetry"), is(nullValue()));
    }

    @Test
    public void fromShouldConvertEventDigestToTransactionForChildResource() throws IOException {
        String parentResourceExternalId = "parent-resource-external-id";
        EventEntity refundCreatedEvent = aQueuePaymentEventFixture()
                .withEventType("REFUND_CREATED_BY_USER")
                .withResourceExternalId("resource-external-id")
                .withParentResourceExternalId(parentResourceExternalId)
                .withResourceType(ResourceType.REFUND)
                .withEventData("{\"refunded_by\": \"refunded-by-id\", \"amount\": 1000}")
                .toEntity();
        EventEntity refundSubmittedEvent = aQueuePaymentEventFixture()
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
        EventEntity paymentCreatedEvent = aQueuePaymentEventFixture().withEventType("PAYMENT_CREATED").toEntity();
        EventEntity nonSalientEvent = aQueuePaymentEventFixture().withEventType("NON_STATE_TRANSITION_EVENT").toEntity();
        EventEntity paymentStartedEvent = aQueuePaymentEventFixture().withEventType("PAYMENT_STARTED").toEntity();
        EventEntity secondNonSalientEvent = aQueuePaymentEventFixture().withEventType("SECOND_NON_STATE_TRANSITION_EVENT").toEntity();

        EventDigest eventDigest = EventDigest.fromEventList(List.of(secondNonSalientEvent, paymentStartedEvent, nonSalientEvent, paymentCreatedEvent));
        TransactionEntity transactionEntity = transactionEntityFactory.create(eventDigest);

        assertThat(transactionEntity.getState().toString(), is("STARTED"));
    }

    @Test
    public void create_ShouldSetUndefinedStateForNoSalientEventTypes() {
        EventEntity paymentDetailsEntered = aQueuePaymentEventFixture().withEventType("PAYMENT_DETAILS_ENTERED").toEntity();

        EventDigest eventDigest = EventDigest.fromEventList(List.of(paymentDetailsEntered));
        TransactionEntity transactionEntity = transactionEntityFactory.create(eventDigest);

        assertThat(transactionEntity.getState().toString(), is("UNDEFINED"));
    }

    @Test
    public void create_ShouldSetCanRetryForTransactionRejectedSalientEventTypesWhenPresent() {
        EventEntity transactionRejected = aQueuePaymentEventFixture()
                .withEventType("AUTHORISATION_REJECTED")
                .withEventData("{\"canRetry\":false}")
                .toEntity();

        EventDigest eventDigest = EventDigest.fromEventList(List.of(transactionRejected));
        TransactionEntity transactionEntity = transactionEntityFactory.create(eventDigest);

        assertThat(transactionEntity.getState().toString(), is("FAILED_REJECTED"));
        JsonObject transactionDetails = JsonParser.parseString(transactionEntity.getTransactionDetails()).getAsJsonObject();
        assertThat(transactionDetails.get("canRetry").getAsBoolean(), is(false));
    }

    @Test
    public void createWithNoSourceParameter_ShouldHandleNullValueCorrectly() {
        EventEntity paymentCreatedEvent = aQueuePaymentEventFixture()
                .withEventType("PAYMENT_CREATED")
                .withEventData("{\"amount\":50}")
                .toEntity();

        EventDigest eventDigest = EventDigest.fromEventList(List.of(paymentCreatedEvent));
        TransactionEntity transactionEntity = transactionEntityFactory.create(eventDigest);

        assertThat(transactionEntity.getSource(), is(nullValue()));
    }

}
