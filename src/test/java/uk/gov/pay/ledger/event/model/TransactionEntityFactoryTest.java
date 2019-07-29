package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

public class TransactionEntityFactoryTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private TransactionEntityFactory transactionEntityFactory;

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        transactionEntityFactory = new TransactionEntityFactory(new ObjectMapper());
    }

    @Test
    public void fromShouldConvertEventDigestToTransactionEntity() {
        Event paymentCreatedEvent = aQueuePaymentEventFixture()
                .withEventType(SalientEventType.PAYMENT_CREATED.name())
                .withDefaultEventDataForEventType(SalientEventType.PAYMENT_CREATED.name())
                .withResourceType(ResourceType.PAYMENT)
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
        EventDigest eventDigest = EventDigest.fromEventList(List.of(paymentCreatedEvent, paymentDetailsEvent, captureConfirmedEvent));

        TransactionEntity transactionEntity = transactionEntityFactory.create(eventDigest);

        assertThat(transactionEntity.getExternalId(), is(eventDigest.getResourceExternalId()));
        assertThat(transactionEntity.getState(), is("created"));
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
        assertThat(transactionEntity.getExternalMetadata(), is(eventDigest.getEventPayload().get("external_metadata")));
        assertThat(transactionEntity.getNetAmount(), is(((Integer)eventDigest.getEventPayload().get("net_amount")).longValue()));
        assertThat(transactionEntity.getTotalAmount(), is(((Integer)eventDigest.getEventPayload().get("total_amount")).longValue()));
        assertThat(transactionEntity.getFee(), is(((Integer)eventDigest.getEventPayload().get("fee")).longValue()));
        assertThat(transactionEntity.getTransactionType(), is("PAYMENT"));

        var expectedTransactionDetails = String.format("{" +
                        "\"language\":\"en\"," +
                        "\"payment_provider\":\"sandbox\"," +
                        "\"expiry_date\":\"11/21\"," +
                        "\"address_line1\":\"12 Rouge Avenue\"," +
                        "\"address_postcode\":\"N1 3QU\"," +
                        "\"address_city\":\"London\"," +
                        "\"address_country\":\"GB\"," +
                        "\"delayed_capture\":false," +
                        "\"return_url\":\"https://example.org\"," +
                        "\"gateway_transaction_id\":\"%s\"," +
                        "\"corporate_surcharge\":5" +
                        "}",
                eventDigest.getEventPayload().get("gateway_transaction_id"));

        assertThat(transactionEntity.getTransactionDetails(), is(expectedTransactionDetails));
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
        assertThat(transactionEntity.getState(), is("submitted"));
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

        assertThat(transactionEntity.getState(), is("started"));
    }
}