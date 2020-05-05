package uk.gov.pay.ledger.payout.model;

import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.pay.ledger.event.model.Event;
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
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private PayoutEntityFactory payoutEntityFactory;

    private GsonBuilder gsonBuilder = new GsonBuilder();

    @Before
    public void setUp() {
        payoutEntityFactory = new PayoutEntityFactory();
    }

    @Test
    public void shouldConvertEventDigestToPayoutEntity() {
        ZonedDateTime paidOutDate = ZonedDateTime.parse("2020-05-04T00:20:00.123456Z");
        Event payoutCreatedEvent = aQueuePaymentEventFixture()
                .withEventType("PAYOUT_CREATED")
                .withEventData(gsonBuilder.create()
                        .toJson(Map.of(
                                "amount", 10000,
                                "statement_descriptor", "SPECIAL TEST SERVICE"
                        ))
                )
                .toEntity();
        Event payoutPaidOutEvent = aQueuePaymentEventFixture()
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
        assertThat(payoutEntity.getAmount(), is(10000L));
        assertThat(payoutEntity.getCreatedDate(), is(payoutCreatedEvent.getEventDate()));
        assertThat(payoutEntity.getPaidOutDate(), is(paidOutDate));
    }

    @Test
    public void shouldDigestStateFromEvents() {
        Event payoutCreatedEvent = aQueuePaymentEventFixture().withEventType("PAYOUT_CREATED").toEntity();
        Event payoutPaidOutEvent = aQueuePaymentEventFixture().withEventType("PAYOUT_PAID_OUT").toEntity();
        EventDigest eventDigest = EventDigest.fromEventList(List.of(payoutPaidOutEvent, payoutCreatedEvent));
        PayoutEntity payoutEntity = payoutEntityFactory.create(eventDigest);

        assertThat(payoutEntity.getStatus(), is(PayoutState.PAID_OUT));
    }
}