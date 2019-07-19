package uk.gov.pay.ledger.transaction.dao;

import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

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
        TransactionFixture fixture = aTransactionFixture()
                    .withDefaultCardDetails()
                    .withNetAmount(55)
                    .withTotalAmount(105)
                    .withDefaultTransactionDetails();
        TransactionEntity transactionEntity = fixture.toEntity();

        transactionDao.upsert(transactionEntity);

        TransactionEntity retrievedTransaction = transactionDao.findTransactionByExternalId(transactionEntity.getExternalId()).get();

        assertThat(retrievedTransaction.getId(), notNullValue());
        assertThat(retrievedTransaction.getGatewayAccountId(), is(transactionEntity.getGatewayAccountId()));
        assertThat(retrievedTransaction.getExternalId(), is(transactionEntity.getExternalId()));
        assertThat(retrievedTransaction.getAmount(), is(transactionEntity.getAmount()));
        assertThat(retrievedTransaction.getReference(), is(transactionEntity.getReference()));
        assertThat(retrievedTransaction.getDescription(), is(transactionEntity.getDescription()));
        assertThat(retrievedTransaction.getState(), is(transactionEntity.getState()));
        assertThat(retrievedTransaction.getEmail(), is(transactionEntity.getEmail()));
        assertThat(retrievedTransaction.getCardholderName(), is(transactionEntity.getCardholderName()));
        assertThat(retrievedTransaction.getExternalMetadata(), is(transactionEntity.getExternalMetadata()));
        assertThat(retrievedTransaction.getCreatedDate(), is(transactionEntity.getCreatedDate()));
        assertThat(retrievedTransaction.getTransactionDetails().contains(fixture.getLanguage()), is(true));
        assertThat(retrievedTransaction.getTransactionDetails().contains(fixture.getReturnUrl()), is(true));
        assertThat(retrievedTransaction.getTransactionDetails().contains(fixture.getPaymentProvider()), is(true));
        assertThat(retrievedTransaction.getTransactionDetails().contains(fixture.getCardDetails().getBillingAddress().getAddressLine1()), is(true));
        assertThat(retrievedTransaction.getEventCount(), is(transactionEntity.getEventCount()));
        assertThat(retrievedTransaction.getCardBrand(), is(transactionEntity.getCardBrand()));
        assertThat(retrievedTransaction.getLastDigitsCardNumber(), is(transactionEntity.getLastDigitsCardNumber()));
        assertThat(retrievedTransaction.getFirstDigitsCardNumber(), is(transactionEntity.getFirstDigitsCardNumber()));
        assertThat(retrievedTransaction.getNetAmount(), is(transactionEntity.getNetAmount()));
        assertThat(retrievedTransaction.getTotalAmount(), is(transactionEntity.getTotalAmount()));
    }

    @Test
    public void shouldUpsertTransaction() {
        TransactionEntity transaction = aTransactionFixture()
                .withState(TransactionState.CREATED)
                .insert(rule.getJdbi())
                .toEntity();


        TransactionEntity modifiedTransaction = aTransactionFixture()
                .withExternalId(transaction.getExternalId())
                .withEventCount(2)
                .withState(TransactionState.SUBMITTED)
                .toEntity();

        transactionDao.upsert(modifiedTransaction);

        TransactionEntity retrievedTransaction = transactionDao.findTransactionByExternalId(transaction.getExternalId()).get();

        assertThat(retrievedTransaction.getState(), is(modifiedTransaction.getState()));
    }

    @Test
    public void shouldNotOverwriteTransactionIfItConsistsOfFewerEvents() {
        TransactionEntity transaction = aTransactionFixture()
                .withEventCount(5)
                .withState(TransactionState.CREATED)
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity modifiedTransaction = aTransactionFixture()
                .withExternalId(transaction.getExternalId())
                .withEventCount(4)
                .withState(TransactionState.SUBMITTED)
                .toEntity();

        transactionDao.upsert(modifiedTransaction);

        TransactionEntity retrievedTransaction = transactionDao.findTransactionByExternalId(transaction.getExternalId()).get();

        assertThat(retrievedTransaction.getState(), is(transaction.getState()));
    }
}