package uk.gov.pay.ledger.payout.dao;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.payout.state.PayoutState;

import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.payout.entity.PayoutEntity.PayoutEntityBuilder.aPayoutEntity;
import static uk.gov.pay.ledger.util.fixture.PayoutFixture.PayoutFixtureBuilder.aPayoutFixture;

public class PayoutDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private PayoutDao payoutDao = new PayoutDao(rule.getJdbi());
    private String gatewayPayoutId;

    @BeforeEach
    public void setUp() {
        gatewayPayoutId = RandomStringUtils.random(6);
    }

    @Test
    public void shouldFetchPayout() {
        var createdDate = now(UTC);
        var paidOutDate = createdDate.minusDays(1);
        var id = RandomStringUtils.randomNumeric(4);
        var gatewayAccountId = RandomStringUtils.randomAlphanumeric(10);
        var serviceId = RandomStringUtils.randomAlphanumeric(10);
        var live = true;

        aPayoutFixture()
                .withId(Long.valueOf(id))
                .withAmount(100L)
                .withCreatedDate(createdDate)
                .withGatewayPayoutId(gatewayPayoutId)
                .withServiceId(serviceId)
                .withLive(live)
                .withPaidOutDate(paidOutDate)
                .withState(PayoutState.PAID_OUT)
                .withEventCount(1)
                .withPayoutDetails("{\"key\": \"value\"}")
                .withGatewayAccountId(gatewayAccountId)
                .build().insert(rule.getJdbi());
        var payout = payoutDao.findByGatewayPayoutId(gatewayPayoutId).get();

        assertThat(payout.getId(), is(Long.valueOf(id)));
        assertThat(payout.getAmount(), is(100L));
        assertThat(payout.getGatewayPayoutId(), is(gatewayPayoutId));
        assertThat(payout.getServiceId(), is(serviceId));
        assertThat(payout.isLive(), is(live));
        assertThat(payout.getState(), is(PayoutState.PAID_OUT));
        assertThat(payout.getCreatedDate(), is(createdDate));
        assertThat(payout.getPaidOutDate(), is(paidOutDate));
        assertThat(payout.getEventCount(), is(1));
        assertThat(payout.getPayoutDetails(), is("{\"key\": \"value\"}"));
        assertThat(payout.getGatewayAccountId(), is(gatewayAccountId));
    }

    @Test
    public void shouldUpsertNonExistentPayout() {
        var createdDate = now(UTC);
        var paidOutDate = createdDate.minusDays(1);

        var payoutEntity = aPayoutEntity()
                .withAmount(100L)
                .withGatewayPayoutId(gatewayPayoutId)
                .withPaidOutDate(paidOutDate)
                .withState(PayoutState.IN_TRANSIT)
                .withCreatedDate(createdDate)
                .withEventCount(1)
                .withPayoutDetails("{\"key\": \"value\"}")
                .build();

        payoutDao.upsert(payoutEntity);

        var payout = payoutDao.findByGatewayPayoutId(gatewayPayoutId).get();
        assertThat(payout.getId(), is(notNullValue()));
        assertThat(payout.getAmount(), is(100L));
        assertThat(payout.getGatewayPayoutId(), is(gatewayPayoutId));
        assertThat(payout.getState(), is(PayoutState.IN_TRANSIT));
        assertThat(payout.getCreatedDate(), is(createdDate));
        assertThat(payout.getPaidOutDate(), is(paidOutDate));
        assertThat(payout.getEventCount(), is(1));
        assertThat(payout.getPayoutDetails(), is("{\"key\": \"value\"}"));
        assertThat(payout.getGatewayAccountId(), is(nullValue()));
    }

    @Test
    public void shouldUpsertExistingPayout() {
        var createdDate = now(UTC);
        var paidOutDate = createdDate.minusDays(1);
        var gatewayAccountId = RandomStringUtils.randomAlphanumeric(10);

        var payoutEntity = aPayoutEntity()
                .withAmount(100L)
                .withGatewayPayoutId(gatewayPayoutId)
                .withPaidOutDate(paidOutDate)
                .withState(PayoutState.IN_TRANSIT)
                .withCreatedDate(createdDate)
                .withEventCount(1)
                .build();

        payoutDao.upsert(payoutEntity);

        var payoutEntity2 = aPayoutEntity()
                .withAmount(1337L)
                .withGatewayPayoutId(gatewayPayoutId)
                .withPaidOutDate(paidOutDate)
                .withState(PayoutState.PAID_OUT)
                .withCreatedDate(createdDate)
                .withEventCount(2)
                .withGatewayAccountId(gatewayAccountId)
                .build();

        payoutDao.upsert(payoutEntity2);

        var payout = payoutDao.findByGatewayPayoutId(gatewayPayoutId).get();

        assertThat(payout.getAmount(), is(1337L));
        assertThat(payout.getGatewayPayoutId(), is(gatewayPayoutId));
        assertThat(payout.getState(), is(PayoutState.PAID_OUT));
        assertThat(payout.getCreatedDate(), is(createdDate));
        assertThat(payout.getPaidOutDate(), is(paidOutDate));
        assertThat(payout.getGatewayAccountId(), is(gatewayAccountId));
    }

    @Test
    public void shouldNotUpsertPayoutIfPayoutConsistsOfFewerEvents() {
        ZonedDateTime createdDate = now(UTC);
        var payoutEntity = aPayoutEntity()
                .withAmount(100L)
                .withGatewayPayoutId(gatewayPayoutId)
                .withState(PayoutState.IN_TRANSIT)
                .withCreatedDate(createdDate)
                .withEventCount(3)
                .build();

        payoutDao.upsert(payoutEntity);

        var payoutEntity2 = aPayoutEntity()
                .withAmount(1337L)
                .withGatewayPayoutId(gatewayPayoutId)
                .withState(PayoutState.PAID_OUT)
                .withCreatedDate(createdDate)
                .withEventCount(2)
                .build();

        payoutDao.upsert(payoutEntity2);

        var payout = payoutDao.findByGatewayPayoutId(gatewayPayoutId).get();

        assertThat(payout.getAmount(), is(100L));
        assertThat(payout.getGatewayPayoutId(), is(gatewayPayoutId));
        assertThat(payout.getState(), is(PayoutState.IN_TRANSIT));
    }
}
