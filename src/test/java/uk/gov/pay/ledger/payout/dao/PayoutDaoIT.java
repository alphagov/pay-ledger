package uk.gov.pay.ledger.payout.dao;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
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

    @Test
    public void shouldFetchTransaction() {
        var createdDate = ZonedDateTime.now(ZoneOffset.UTC);
        var paidOutDate = createdDate.minusDays(1);
        var id = RandomStringUtils.randomNumeric(4);
        aPayoutFixture()
                .withId(Long.valueOf(id))
                .withAmount(100L)
                .withCreatedDate(createdDate)
                .withGatewayPayoutId("po_asdasdasd")
                .withPaidOutDate(paidOutDate)
                .withStatementDescriptor("a statement descriptor")
                .withStatus("PAYOUT")
                .withType("Bank Account")
                .build().insert(rule.getJdbi());
        var payout = payoutDao.findByGatewayPayoutId("po_asdasdasd").get();
        assertThat(payout.getId(), is(Long.valueOf(id)));
        assertThat(payout.getAmount(), is(100L));
        assertThat(payout.getGatewayPayoutId(), is("po_asdasdasd"));
        assertThat(payout.getStatementDescriptor(), is("a statement descriptor"));
        assertThat(payout.getStatus(), is("PAYOUT"));
        assertThat(payout.getType(), is("Bank Account"));
        assertThat(payout.getCreatedDate(), is(notNullValue()));
        assertThat(payout.getPaidOutDate(), is(paidOutDate));
    }

    @Test
    public void shouldUpsertNonExistentTransaction(){
        var createdDate = ZonedDateTime.now(ZoneOffset.UTC);
        var paidOutDate = createdDate.minusDays(1);

        var payoutEntity = aPayoutEntity()
                .withAmount(100L)
                .withGatewayPayoutId("po_test2")
                .withPaidOutDate(paidOutDate)
                .withStatementDescriptor("a statement descriptor")
                .withStatus("PAYOUT")
                .withType("Bank Account")
                .build();

        payoutDao.upsert(payoutEntity);

        var payout = payoutDao.findByGatewayPayoutId("po_test2").get();
        assertThat(payout.getId(), is(1L));
        assertThat(payout.getAmount(), is(100L));
        assertThat(payout.getGatewayPayoutId(), is("po_test2"));
        assertThat(payout.getStatementDescriptor(), is("a statement descriptor"));
        assertThat(payout.getStatus(), is("PAYOUT"));
        assertThat(payout.getType(), is("Bank Account"));
        assertThat(payout.getCreatedDate(), is(notNullValue()));
        assertThat(payout.getPaidOutDate(), is(paidOutDate));
    }

    @Test
    public void shouldUpsertExistingTransaction() {
        var createdDate = ZonedDateTime.now(ZoneOffset.UTC);
        var paidOutDate = createdDate.minusDays(1);

        var payoutEntity = aPayoutEntity()
                .withAmount(100L)
                .withGatewayPayoutId("po_test2")
                .withPaidOutDate(paidOutDate)
                .withStatementDescriptor("a statement descriptor")
                .withStatus("PAYOUT")
                .withType("Bank Account")
                .build();

        payoutDao.upsert(payoutEntity);

        var payoutEntity2 = aPayoutEntity()
                .withAmount(1337L)
                .withGatewayPayoutId("po_test2")
                .withPaidOutDate(paidOutDate)
                .withStatementDescriptor("a descriptor")
                .withStatus("PAID")
                .withType("Test Account")
                .build();

        payoutDao.upsert(payoutEntity2);

        var payout = payoutDao.findByGatewayPayoutId("po_test2").get();

        assertThat(payout.getAmount(), is(1337L));
        assertThat(payout.getGatewayPayoutId(), is("po_test2"));
        assertThat(payout.getStatementDescriptor(), is("a descriptor"));
        assertThat(payout.getStatus(), is("PAID"));
        assertThat(payout.getType(), is("Test Account"));
        assertThat(payout.getCreatedDate(), is(notNullValue()));
        assertThat(payout.getPaidOutDate(), is(paidOutDate));
    }
}
