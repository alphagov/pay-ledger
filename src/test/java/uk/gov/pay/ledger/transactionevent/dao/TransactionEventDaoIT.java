package uk.gov.pay.ledger.transactionevent.dao;

import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transactionevent.model.TransactionEventEntity;
import uk.gov.pay.ledger.util.fixture.EventFixture;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class TransactionEventDaoIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private TransactionEventDao transactionEventDao = new TransactionEventDao(rule.getJdbi());

    @Test
    public void shouldCorrectlyMapDBColumnsToTransactionEventEntity() {
        EventFixture eventFixture = EventFixture.anEventFixture()
                .insert(rule.getJdbi());

        TransactionFixture transactionFixture = aTransactionFixture()
                .withExternalId(eventFixture.getResourceExternalId())
                .withAmount(100L)
                .withTransactionType("PAYMENT")
                .insert(rule.getJdbi());

        TransactionEventEntity transactionEventEntity =
                transactionEventDao.getTransactionEventsByExternalIdAndGatewayAccountId(
                        transactionFixture.getExternalId(), transactionFixture.getGatewayAccountId()).get(0);

        assertThat(transactionEventEntity.getAmount(), is(100L));
        assertThat(transactionEventEntity.getExternalId(), is(transactionFixture.getExternalId()));
        assertThat(transactionEventEntity.getTransactionType(), is(transactionFixture.getTransactionType()));
        assertThat(transactionEventEntity.getEventData(), is(eventFixture.getEventData()));
        assertThat(transactionEventEntity.getEventDate(), is(eventFixture.getEventDate()));
        assertThat(transactionEventEntity.getEventType(), is(eventFixture.getEventType()));
    }

    @Test
    public void shouldFilterEventsByExternalIdAndGatewayAccountId() {
        EventFixture eventFixture1 = EventFixture.anEventFixture()
                .withResourceExternalId("external-id-1")
                .withEventType("CAPTURE_APPROVED")
                .withEventData("{\"fee\": 10}")
                .insert(rule.getJdbi());
        TransactionFixture transactionFixture1 = aTransactionFixture()
                .withExternalId(eventFixture1.getResourceExternalId())
                .withAmount(100L)
                .withTransactionType("PAYMENT")
                .insert(rule.getJdbi());

        EventFixture eventFixture2 = EventFixture.anEventFixture()
                .withResourceExternalId("external-id-2")
                .withEventType("PAYMENT_CREATED")
                .insert(rule.getJdbi());
        aTransactionFixture()
                .withExternalId(eventFixture2.getResourceExternalId())
                .withTransactionType("REFUND")
                .insert(rule.getJdbi());

        TransactionEventEntity transactionEventEntity =
                transactionEventDao.getTransactionEventsByExternalIdAndGatewayAccountId(
                        transactionFixture1.getExternalId(), transactionFixture1.getGatewayAccountId()).get(0);

        assertThat(transactionEventEntity.getAmount(), is(100L));
        assertThat(transactionEventEntity.getExternalId(), is("external-id-1"));
        assertThat(transactionEventEntity.getTransactionType(), is("PAYMENT"));
        assertThat(transactionEventEntity.getEventData(), is("{\"fee\": 10}"));
        assertThat(transactionEventEntity.getEventDate(), is(eventFixture1.getEventDate()));
        assertThat(transactionEventEntity.getEventType(), is("CAPTURE_APPROVED"));
    }

    @Test
    public void shouldFilterEventsByParentExternalId() {
        EventFixture eventFixtureParent = EventFixture.anEventFixture()
                .withResourceExternalId("parent-external-id")
                .withEventType("PAYMENT_CREATED")
                .insert(rule.getJdbi());

        TransactionFixture transactionFixtureParent = aTransactionFixture()
                .withExternalId(eventFixtureParent.getResourceExternalId())
                .withTransactionType("PAYMENT")
                .insert(rule.getJdbi());

        EventFixture eventFixtureChild = EventFixture.anEventFixture()
                .withResourceExternalId("related-external-id")
                .withEventType("REFUND_CREATED")
                .withEventDate(eventFixtureParent.getEventDate().plusDays(1)) // to preserve ordering of DB results
                .insert(rule.getJdbi());
        TransactionFixture transactionFixtureChild = aTransactionFixture()
                .withExternalId(eventFixtureChild.getResourceExternalId())
                .withGatewayAccountId(transactionFixtureParent.getGatewayAccountId())
                .withParentExternalId(transactionFixtureParent.getExternalId())
                .withTransactionType("REFUND")
                .insert(rule.getJdbi());

        List<TransactionEventEntity> transactionEventEntityList =
                transactionEventDao.getTransactionEventsByExternalIdAndGatewayAccountId(
                        eventFixtureParent.getResourceExternalId(), transactionFixtureChild.getGatewayAccountId());

        assertThat(transactionEventEntityList.size(), is(2));

        assertThat(transactionEventEntityList.get(0).getAmount(), is(transactionFixtureParent.getAmount()));
        assertThat(transactionEventEntityList.get(0).getExternalId(), is(transactionFixtureParent.getExternalId()));
        assertThat(transactionEventEntityList.get(0).getTransactionType(), is(transactionFixtureParent.getTransactionType()));
        assertThat(transactionEventEntityList.get(0).getEventData(), is(eventFixtureParent.getEventData()));
        assertThat(transactionEventEntityList.get(0).getEventDate(), is(eventFixtureParent.getEventDate()));
        assertThat(transactionEventEntityList.get(0).getEventType(), is(eventFixtureParent.getEventType()));

        assertThat(transactionEventEntityList.get(1).getAmount(), is(transactionFixtureChild.getAmount()));
        assertThat(transactionEventEntityList.get(1).getExternalId(), is(transactionFixtureChild.getExternalId()));
        assertThat(transactionEventEntityList.get(1).getTransactionType(), is(transactionFixtureChild.getTransactionType()));
        assertThat(transactionEventEntityList.get(1).getEventData(), is(eventFixtureChild.getEventData()));
        assertThat(transactionEventEntityList.get(1).getEventDate(), is(eventFixtureChild.getEventDate()));
        assertThat(transactionEventEntityList.get(1).getEventType(), is(eventFixtureChild.getEventType()));
    }
}
