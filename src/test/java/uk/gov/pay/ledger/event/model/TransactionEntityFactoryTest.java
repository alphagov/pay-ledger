package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.fixture.QueueEventFixture.aQueueEventFixture;

public class TransactionEntityFactoryTest {

    private TransactionEntityFactory transactionEntityFactory;

    @Before
    public void setUp() {
        transactionEntityFactory = new TransactionEntityFactory(new ObjectMapper());
    }

    @Test
    public void fromShouldConvertEventDigestToTransactionEntity() {
        Event paymentCreatedEvent = aQueueEventFixture()
                .withEventType(EventType.PAYMENT_CREATED.name())
                .withDefaultEventDataForEventType(EventType.PAYMENT_CREATED.name())
                .toEntity();
        Event paymentDetailsEvent = aQueueEventFixture()
                .withEventType("PAYMENT_DETAILS_ENTERED")
                .withDefaultEventDataForEventType("PAYMENT_DETAILS_ENTERED")
                .toEntity();
        Event captureConfirmedEvent = aQueueEventFixture()
                .withEventType("CAPTURE_CONFIRMED")
                .withEventData("{\"net_amount\": 55, \"total_amount\": 105}")
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

        var expectedTransactionDetails = String.format("{\"language\":\"en\",\"payment_provider\":\"sandbox\",\"expiry_date\":\"11/21\",\"address_line1\":\"12 Rouge Avenue\",\"address_line2\":null,\"address_postcode\":\"N1 3QU\",\"address_city\":\"London\",\"address_county\":null,\"address_country\":\"GB\",\"wallet\":null,\"delayed_capture\":false,\"return_url\":\"https://example.org\",\"gateway_transaction_id\":\"%s\"}",
                eventDigest.getEventPayload().get("gateway_transaction_id"));

        assertThat(transactionEntity.getTransactionDetails(), is(expectedTransactionDetails));
    }
}