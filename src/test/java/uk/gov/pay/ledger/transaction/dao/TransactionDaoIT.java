package uk.gov.pay.ledger.transaction.dao;

import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import java.util.List;

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
                .withFee(33)
                .withTransactionType("PAYMENT")
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
        assertThat(retrievedTransaction.getFee(), is(transactionEntity.getFee()));
        assertThat(retrievedTransaction.getTransactionType(), is(transactionEntity.getTransactionType()));
        assertThat(retrievedTransaction.getRefundAmountAvailable(), is(transactionEntity.getRefundAmountAvailable()));
        assertThat(retrievedTransaction.getRefundAmountSubmitted(), is(transactionEntity.getRefundAmountSubmitted()));
        assertThat(retrievedTransaction.getRefundStatus(), is(transactionEntity.getRefundStatus()));
    }

    @Test
    public void shouldRetrieveTransactionByExternalIdAndGatewayAccountId() {
        TransactionFixture fixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withTransactionType("PAYMENT")
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi());
        TransactionEntity transactionEntity = fixture.toEntity();

        TransactionEntity transaction = transactionDao
                .findTransactionByExternalIdAndGatewayAccountId(
                        transactionEntity.getExternalId(),
                        transactionEntity.getGatewayAccountId()
                ).get();

        assertThat(transaction.getId(), notNullValue());
        assertThat(transaction.getGatewayAccountId(), is(transactionEntity.getGatewayAccountId()));
        assertThat(transaction.getExternalId(), is(transactionEntity.getExternalId()));
        assertThat(transaction.getAmount(), is(transactionEntity.getAmount()));
        assertThat(transaction.getReference(), is(transactionEntity.getReference()));
        assertThat(transaction.getDescription(), is(transactionEntity.getDescription()));
        assertThat(transaction.getState(), is(transactionEntity.getState()));
        assertThat(transaction.getEmail(), is(transactionEntity.getEmail()));
        assertThat(transaction.getCardholderName(), is(transactionEntity.getCardholderName()));
        assertThat(transaction.getCreatedDate(), is(transactionEntity.getCreatedDate()));
        assertThat(transaction.getTransactionDetails().contains(fixture.getLanguage()), is(true));
        assertThat(transaction.getTransactionDetails().contains(fixture.getReturnUrl()), is(true));
        assertThat(transaction.getTransactionDetails().contains(fixture.getPaymentProvider()), is(true));
        assertThat(transaction.getTransactionDetails().contains(fixture.getCardDetails().getBillingAddress().getAddressLine1()), is(true));
        assertThat(transaction.getEventCount(), is(transactionEntity.getEventCount()));
        assertThat(transaction.getCardBrand(), is(transactionEntity.getCardBrand()));
        assertThat(transaction.getLastDigitsCardNumber(), is(transactionEntity.getLastDigitsCardNumber()));
        assertThat(transaction.getFirstDigitsCardNumber(), is(transactionEntity.getFirstDigitsCardNumber()));
        assertThat(transaction.getTransactionType(), is(transactionEntity.getTransactionType()));
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

    @Test
    public void shouldFilterTransactionByExternalIdOrParentExternalIdAndGatewayAccountId() {
        TransactionEntity transaction1 = aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withExternalId("external-id-1")
                .withGatewayAccountId("gateway-account-id-1")
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity transaction2 = aTransactionFixture()
                .withState(TransactionState.SUBMITTED)
                .withExternalId("external-id-2")
                .withGatewayAccountId("gateway-account-id-2")
                .insert(rule.getJdbi())
                .toEntity();

        List<TransactionEntity> transactionEntityList =
                transactionDao.findTransactionByExternalOrParentIdAndGatewayAccountId(
                        transaction1.getExternalId(), transaction1.getGatewayAccountId());

        assertThat(transactionEntityList.size(), is(1));
        assertThat(transactionEntityList.get(0).getExternalId(), is(transaction1.getExternalId()));
        assertThat(transactionEntityList.get(0).getGatewayAccountId(), is(transaction1.getGatewayAccountId()));
    }

    @Test
    public void findTransactionByExternalOrParentIdAndGatewayAccountId_shouldFilterByParentExternalId() {
        TransactionEntity transaction1 = aTransactionFixture()
                .withState(TransactionState.CREATED)
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity transaction2 = aTransactionFixture()
                .withState(TransactionState.SUBMITTED)
                .withGatewayAccountId(transaction1.getGatewayAccountId())
                .withParentExternalId(transaction1.getExternalId())
                .insert(rule.getJdbi())
                .toEntity();

        List<TransactionEntity> transactionEntityList =
                transactionDao.findTransactionByExternalOrParentIdAndGatewayAccountId(
                        transaction1.getExternalId(), transaction1.getGatewayAccountId());

        assertThat(transactionEntityList.size(), is(2));
    }

    @Test
    public void findTransactionByParentIdAndGatewayAccountId_shouldFilterByParentExternalId() {
        TransactionEntity transaction1 = aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withTransactionType("PAYMENT")
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity transactionWithParentExternalId = aTransactionFixture()
                .withState(TransactionState.SUBMITTED)
                .withTransactionType("REFUND")
                .withGatewayAccountId(transaction1.getGatewayAccountId())
                .withParentExternalId(transaction1.getExternalId())
                .insert(rule.getJdbi())
                .toEntity();

        List<TransactionEntity> transactionEntityList =
                transactionDao.findTransactionByParentIdAndGatewayAccountId(
                        transactionWithParentExternalId.getParentExternalId(),
                        transactionWithParentExternalId.getGatewayAccountId());

        assertThat(transactionEntityList.size(), is(1));
        TransactionEntity transactionEntity = transactionEntityList.get(0);

        assertThat(transactionEntity.getId(), is(transactionWithParentExternalId.getId()));

    }
}