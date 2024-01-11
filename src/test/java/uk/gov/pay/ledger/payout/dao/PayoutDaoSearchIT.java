package uk.gov.pay.ledger.payout.dao;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.payout.search.PayoutSearchParams;
import uk.gov.pay.ledger.payout.state.PayoutState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.PayoutFixture.PayoutFixtureBuilder.aPayoutFixture;

public class PayoutDaoSearchIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private PayoutDao payoutDao;
    private PayoutSearchParams searchParams;

    private DatabaseTestHelper databaseTestHelper = aDatabaseTestHelper(rule.getJdbi());

    @BeforeEach
    public void setUp() {
        databaseTestHelper.truncateAllPayoutData();
        payoutDao = new PayoutDao(rule.getJdbi());
        searchParams = new PayoutSearchParams();
    }

    @Test
    public void shouldGetAndMapPayoutCorrectly() {
        PayoutEntity payoutEntity = aPayoutFixture()
                .build()
                .insert(rule.getJdbi())
                .toEntity();

        List<PayoutEntity> payoutList = payoutDao.searchPayouts(searchParams);
        assertThat(payoutList.size(), is(1));
        PayoutEntity retrievedEntity = payoutList.get(0);
        assertThat(retrievedEntity.getAmount(), is(payoutEntity.getAmount()));
        assertThat(retrievedEntity.getState().getStatus(), is(payoutEntity.getState().getStatus()));
        assertThat(retrievedEntity.getCreatedDate(), is(payoutEntity.getCreatedDate()));
        assertThat(retrievedEntity.getGatewayPayoutId(), is(payoutEntity.getGatewayPayoutId()));
    }

    @Test
    public void shouldFilterOnSpecificGatewayAccountId() {
        PayoutEntity payoutEntity1 = aPayoutFixture()
                .build()
                .insert(rule.getJdbi())
                .toEntity();

        PayoutEntity payoutEntity2 = aPayoutFixture()
                .build()
                .insert(rule.getJdbi())
                .toEntity();

        searchParams.setGatewayAccountIds(Set.of(payoutEntity2.getGatewayAccountId()));

        List<PayoutEntity> payoutList = payoutDao.searchPayouts(searchParams);
        assertThat(payoutList.size(), is(1));
        assertThat(payoutList.get(0).getGatewayAccountId(), is(payoutEntity2.getGatewayAccountId()));
    }

    @Test
    public void shouldFilterOnSpecificState() {
        PayoutEntity payoutEntity1 = aPayoutFixture()
                .withState(PayoutState.PAID_OUT)
                .build()
                .insert(rule.getJdbi())
                .toEntity();

        PayoutEntity payoutEntity2 = aPayoutFixture()
                .withState(PayoutState.FAILED)
                .build()
                .insert(rule.getJdbi())
                .toEntity();

        PayoutEntity payoutEntity3 = aPayoutFixture()
                .withState(PayoutState.IN_TRANSIT)
                .build()
                .insert(rule.getJdbi())
                .toEntity();

        searchParams.setState(PayoutState.IN_TRANSIT.getStatus());

        List<PayoutEntity> payoutList = payoutDao.searchPayouts(searchParams);
        assertThat(payoutList.size(), is(1));
        assertThat(payoutList.get(0).getGatewayAccountId(), is(payoutEntity3.getGatewayAccountId()));
    }

    @Test
    public void shouldReturn10Records_withPageSize10() {
        String gatewayAccountId = RandomStringUtils.randomAlphanumeric(10);

        for (int i = 0; i < 15; i++) {
            aPayoutFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .build()
                    .insert(rule.getJdbi());
        }

        searchParams.setGatewayAccountIds(Set.of(gatewayAccountId));
        searchParams.setDisplaySize(10L);

        searchParams.setState(PayoutState.PAID_OUT.getStatus());

        List<PayoutEntity> payoutList = payoutDao.searchPayouts(searchParams);
        assertThat(payoutList.size(), is(10));

        Long total = payoutDao.getTotalForSearch(searchParams);
        assertThat(total, is(15L));
    }

    @Test
    public void shouldReturn5Records_withOffsetAndPageSizeSet() {
        String gatewayAccountId = RandomStringUtils.randomAlphanumeric(10);

        for (int i = 0; i < 15; i++) {
            aPayoutFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .build()
                    .insert(rule.getJdbi());
        }

        searchParams.setGatewayAccountIds(Set.of(gatewayAccountId));
        searchParams.setDisplaySize(10L);
        searchParams.setPageNumber(2l);

        searchParams.setState(PayoutState.PAID_OUT.getStatus());

        List<PayoutEntity> payoutList = payoutDao.searchPayouts(searchParams);
        assertThat(payoutList.size(), is(5));

        Long total = payoutDao.getTotalForSearch(searchParams);
        assertThat(total, is(15L));
    }

    @Test
    public void shouldFilter_withMultipleGatewayAccountIdsSpecified() {
        String gatewayAccountId1 = RandomStringUtils.randomAlphanumeric(10);
        String gatewayAccountId2 = RandomStringUtils.randomAlphanumeric(10);
        String gatewayAccountId3 = RandomStringUtils.randomAlphanumeric(10);
        String gatewayAccountId4 = RandomStringUtils.randomAlphanumeric(10);

        for (int i = 0; i < 5; i++) {
            aPayoutFixture()
                    .withGatewayAccountId(gatewayAccountId1)
                    .build()
                    .insert(rule.getJdbi());
        }

        for (int i = 0; i < 5; i++) {
            aPayoutFixture()
                    .withGatewayAccountId(gatewayAccountId2)
                    .build()
                    .insert(rule.getJdbi());
        }

        for (int i = 0; i < 5; i++) {
            aPayoutFixture()
                    .withGatewayAccountId(gatewayAccountId3)
                    .build()
                    .insert(rule.getJdbi());
        }

        for (int i = 0; i < 5; i++) {
            aPayoutFixture()
                    .withGatewayAccountId(gatewayAccountId4)
                    .build()
                    .insert(rule.getJdbi());
        }

        searchParams.setGatewayAccountIds(Set.of(gatewayAccountId1, gatewayAccountId4));
        List<PayoutEntity> payoutList = payoutDao.searchPayouts(searchParams);
        assertThat(payoutList.size(), is(10));
        assertThat(payoutList.get(0).getGatewayAccountId(), is(gatewayAccountId4));
        assertThat(payoutList.get(5).getGatewayAccountId(), is(gatewayAccountId1));
    }
}
