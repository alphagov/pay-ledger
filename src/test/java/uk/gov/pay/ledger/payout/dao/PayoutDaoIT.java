package uk.gov.pay.ledger.payout.dao;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import uk.gov.pay.ledger.payout.state.PayoutState;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.payout.entity.PayoutEntity.PayoutEntityBuilder.aPayoutEntity;
import static uk.gov.pay.ledger.util.fixture.PayoutFixture.PayoutFixtureBuilder.aPayoutFixture;

public class PayoutDaoIT {
    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private PayoutDao payoutDao = new PayoutDao(rule.getJdbi());
    private String gatewayPayoutId;

    @Before
    public void setUp(){
        gatewayPayoutId = RandomStringUtils.random(6);
    }

    @Test
    public void shouldFetchPayout() {
        var createdDate = ZonedDateTime.now(ZoneOffset.UTC);
        var paidOutDate = createdDate.minusDays(1);
        var id = RandomStringUtils.randomNumeric(4);
        aPayoutFixture()
                .withId(Long.valueOf(id))
                .withAmount(100L)
                .withCreatedDate(createdDate)
                .withGatewayPayoutId(gatewayPayoutId)
                .withPaidOutDate(paidOutDate)
                .withState(PayoutState.PAID_OUT)
                .withEventCount(1)
                .withPayoutDetails("{\"key\": \"value\"}")
                .build().insert(rule.getJdbi());
        var payout = payoutDao.findByGatewayPayoutId(gatewayPayoutId).get();
        assertThat(payout.getId(), is(Long.valueOf(id)));
        assertThat(payout.getAmount(), is(100L));
        assertThat(payout.getGatewayPayoutId(), is(gatewayPayoutId));
        assertThat(payout.getState(), is(PayoutState.PAID_OUT));
        assertThat(payout.getCreatedDate(), is(notNullValue()));
        assertThat(payout.getPaidOutDate(), is(paidOutDate));
        assertThat(payout.getEventCount(), is(1));
        assertThat(payout.getPayoutDetails(), is("{\"key\": \"value\"}"));
    }

    @Test
    public void shouldUpsertNonExistentPayout(){
        var createdDate = ZonedDateTime.now(ZoneOffset.UTC);
        var paidOutDate = createdDate.minusDays(1);

        var payoutEntity = aPayoutEntity()
                .withAmount(100L)
                .withGatewayPayoutId(gatewayPayoutId)
                .withPaidOutDate(paidOutDate)
                .withState(PayoutState.IN_TRANSIT)
                .withEventCount(1)
                .withPayoutDetails("{\"key\": \"value\"}")
                .build();

        payoutDao.upsert(payoutEntity);

        var payout = payoutDao.findByGatewayPayoutId(gatewayPayoutId).get();
        assertThat(payout.getId(), is(1L));
        assertThat(payout.getAmount(), is(100L));
        assertThat(payout.getGatewayPayoutId(), is(gatewayPayoutId));
        assertThat(payout.getState(), is(PayoutState.IN_TRANSIT));
        assertThat(payout.getCreatedDate(), is(notNullValue()));
        assertThat(payout.getPaidOutDate(), is(paidOutDate));
        assertThat(payout.getEventCount(), is(1));
        assertThat(payout.getPayoutDetails(), is("{\"key\": \"value\"}"));
    }

    @Test
    public void shouldUpsertExistingPayout() {
        var createdDate = ZonedDateTime.now(ZoneOffset.UTC);
        var paidOutDate = createdDate.minusDays(1);

        var payoutEntity = aPayoutEntity()
                .withAmount(100L)
                .withGatewayPayoutId(gatewayPayoutId)
                .withPaidOutDate(paidOutDate)
                .withState(PayoutState.IN_TRANSIT)
                .withEventCount(1)
                .build();

        payoutDao.upsert(payoutEntity);

        var payoutEntity2 = aPayoutEntity()
                .withAmount(1337L)
                .withGatewayPayoutId(gatewayPayoutId)
                .withPaidOutDate(paidOutDate)
                .withState(PayoutState.PAID_OUT)
                .withEventCount(2)
                .build();

        payoutDao.upsert(payoutEntity2);

        var payout = payoutDao.findByGatewayPayoutId(gatewayPayoutId).get();

        assertThat(payout.getAmount(), is(1337L));
        assertThat(payout.getGatewayPayoutId(), is(gatewayPayoutId));
        assertThat(payout.getState(), is(PayoutState.PAID_OUT));
        assertThat(payout.getCreatedDate(), is(notNullValue()));
        assertThat(payout.getPaidOutDate(), is(paidOutDate));
    }

    @Test
    public void shouldNotUpsertPayoutIfPayoutConsistsOfFewerEvents() {
        var payoutEntity = aPayoutEntity()
                .withAmount(100L)
                .withGatewayPayoutId(gatewayPayoutId)
                .withState(PayoutState.IN_TRANSIT)
                .withEventCount(3)
                .build();

        payoutDao.upsert(payoutEntity);

        var payoutEntity2 = aPayoutEntity()
                .withAmount(1337L)
                .withGatewayPayoutId(gatewayPayoutId)
                .withState(PayoutState.PAID_OUT)
                .withEventCount(2)
                .build();

        payoutDao.upsert(payoutEntity2);

        var payout = payoutDao.findByGatewayPayoutId(gatewayPayoutId).get();

        assertThat(payout.getAmount(), is(100L));
        assertThat(payout.getGatewayPayoutId(), is(gatewayPayoutId));
        assertThat(payout.getState(), is(PayoutState.IN_TRANSIT));
    }
}
