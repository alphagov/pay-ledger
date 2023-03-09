package uk.gov.pay.ledger.payout.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.ledger.event.dao.entity.EventEntity;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.payout.state.PayoutState;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

public class PayoutEntityFactoryTest {

    private PayoutEntityFactory payoutEntityFactory;

    private GsonBuilder gsonBuilder = new GsonBuilder();
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        payoutEntityFactory = new PayoutEntityFactory(objectMapper);
    }

    @Test
    public void shouldConvertEventDigestToPayoutEntity() {
        ZonedDateTime paidOutDate = ZonedDateTime.parse("2020-05-04T00:20:00.123456Z");
        EventEntity payoutCreatedEvent = aQueuePaymentEventFixture()
                .withEventType("PAYOUT_CREATED")
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "amount", 10000,
                                "statement_descriptor", "SPECIAL TEST SERVICE"
                        ))
                )
                .toEntity();
        EventEntity payoutPaidOutEvent = aQueuePaymentEventFixture()
                .withEventType("PAYOUT_PAID_OUT")
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "paid_out_date", paidOutDate.toString()
                        ))
                )
                .toEntity();
        EventDigest eventDigest = EventDigest.fromEventList(List.of(payoutCreatedEvent, payoutPaidOutEvent));
        PayoutEntity payoutEntity = payoutEntityFactory.create(eventDigest);

        assertThat(payoutEntity.getGatewayPayoutId(), is(payoutCreatedEvent.getResourceExternalId()));
        assertThat(payoutEntity.getServiceId(), is(payoutCreatedEvent.getServiceId()));
        assertThat(payoutEntity.getLive(), is(payoutCreatedEvent.getLive()));
        assertThat(payoutEntity.getAmount(), is(10000L));
        assertThat(payoutEntity.getCreatedDate(), is(payoutCreatedEvent.getEventDate()));
        assertThat(payoutEntity.getPaidOutDate(), is(paidOutDate));
        assertThat(payoutEntity.getEventCount(), is(2));

        JsonObject payoutDetails = JsonParser.parseString(payoutEntity.getPayoutDetails()).getAsJsonObject();
        assertThat(payoutDetails.get("amount").getAsInt(), is(10000));
        assertThat(payoutDetails.get("statement_descriptor").getAsString(), is("SPECIAL TEST SERVICE"));
        assertThat(payoutDetails.get("paid_out_date").getAsString(), is(paidOutDate.toString()));
    }

    @Test
    public void shouldDigestStateFromEvents() {
        EventEntity payoutCreatedEvent = aQueuePaymentEventFixture().withEventType("PAYOUT_CREATED").toEntity();
        EventEntity payoutPaidOutEvent = aQueuePaymentEventFixture().withEventType("PAYOUT_PAID").toEntity();
        EventDigest eventDigest = EventDigest.fromEventList(List.of(payoutPaidOutEvent, payoutCreatedEvent));
        PayoutEntity payoutEntity = payoutEntityFactory.create(eventDigest);

        assertThat(payoutEntity.getState(), is(PayoutState.PAID_OUT));
    }
}
