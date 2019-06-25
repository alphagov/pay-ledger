package uk.gov.pay.ledger.transaction.dao;

import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class TransactionDaoIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private TransactionDao transactionDao = new TransactionDao(rule.getJdbi());

    @Test
    public void shouldInsertTransaction() {
        Transaction transaction = aTransactionFixture()
                .toEntity();

        transactionDao.upsert(transaction);

        Transaction retrievedTransaction = transactionDao.findTransactionByExternalId(transaction.getExternalId()).get();

        assertThat(retrievedTransaction.getId(), notNullValue());
        assertThat(retrievedTransaction.getExternalId(), is(transaction.getExternalId()));
        assertThat(retrievedTransaction.getCardDetails(), is(transaction.getCardDetails()));
        assertThat(retrievedTransaction.getReference(), is(transaction.getReference()));
        assertThat(retrievedTransaction.getGatewayAccountId(), is(transaction.getGatewayAccountId()));
        assertThat(retrievedTransaction.getEmail(), is(transaction.getEmail()));
        assertThat(retrievedTransaction.getAmount(), is(transaction.getAmount()));
        assertThat(retrievedTransaction.getCreatedDate(), is(transaction.getCreatedDate()));
        assertThat(retrievedTransaction.getDelayedCapture(), is(transaction.getDelayedCapture()));
        assertThat(retrievedTransaction.getLanguage(), is(transaction.getLanguage()));
        assertThat(retrievedTransaction.getPaymentProvider(), is(transaction.getPaymentProvider()));
        assertThat(retrievedTransaction.getReturnUrl(), is(transaction.getReturnUrl()));
        assertThat(retrievedTransaction.getState(), is(transaction.getState()));
        assertThat(retrievedTransaction.getExternalMetadata(), is(transaction.getExternalMetadata()));
    }

    @Test
    public void shouldUpsertTransaction() {
        Transaction transaction = aTransactionFixture()
                .insert(rule.getJdbi())
                .toEntity();


        Transaction modifiedTransaction = aTransactionFixture()
                .withExternalId(transaction.getExternalId())
                .withEventCount(2)
                .withState(TransactionState.SUBMITTED)
                .toEntity();

        transactionDao.upsert(modifiedTransaction);

        Transaction retrievedTransaction = transactionDao.findTransactionByExternalId(transaction.getExternalId()).get();

        assertThat(retrievedTransaction.getState(), is(modifiedTransaction.getState()));
    }

    @Test
    public void shouldNotOverwriteTransactionIfItConsistsOfFewerEvents() {
        Transaction transaction = aTransactionFixture()
                .withEventCount(5)
                .insert(rule.getJdbi())
                .toEntity();

        Transaction modifiedTransaction = aTransactionFixture()
                .withExternalId(transaction.getExternalId())
                .withEventCount(4)
                .withState(TransactionState.SUBMITTED)
                .toEntity();

        transactionDao.upsert(modifiedTransaction);

        Transaction retrievedTransaction = transactionDao.findTransactionByExternalId(transaction.getExternalId()).get();

        assertThat(retrievedTransaction.getState(), is(transaction.getState()));
    }
}