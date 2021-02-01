package uk.gov.pay.ledger.transaction.dao;

import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.commons.model.Source;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.fixture.PayoutFixture.PayoutFixtureBuilder.aPayoutFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

class TransactionDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private TransactionDao transactionDao = new TransactionDao(rule.getJdbi());

    @Test
    void shouldInsertTransaction() {
        TransactionFixture fixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withNetAmount(55)
                .withTotalAmount(105)
                .withFee(33)
                .withExternalMetadata(ImmutableMap.of("key1", "value1", "anotherKey", ImmutableMap.of("nestedKey", "value")))
                .withTransactionType("PAYMENT")
                .withLive(true)
                .withGatewayTransactionId("gateway_transaction_id")
                .withGatewayPayoutId("payout-id")
                .withDefaultTransactionDetails();
        TransactionEntity transactionEntity = fixture.toEntity();

        transactionDao.upsert(transactionEntity);

        TransactionEntity retrievedTransaction = transactionDao.findTransactionByExternalId(transactionEntity.getExternalId()).get();

        assertTransactionEntity(retrievedTransaction, fixture);
        assertThat(retrievedTransaction.getNetAmount(), is(transactionEntity.getNetAmount()));
        assertThat(retrievedTransaction.getTotalAmount(), is(transactionEntity.getTotalAmount()));
        assertThat(retrievedTransaction.getFee(), is(transactionEntity.getFee()));
        assertThat(retrievedTransaction.isLive(), is(true));
    }

    @Test
    void shouldInsertTransactionWithSourceCardApi() {
        TransactionFixture fixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withNetAmount(55)
                .withTotalAmount(55)
                .withTransactionType("PAYMENT")
                .withLive(true)
                .withSource(Source.CARD_API.toString())
                .withDefaultTransactionDetails();
        TransactionEntity transactionEntity = fixture.toEntity();

        transactionDao.upsert(transactionEntity);

        TransactionEntity retrievedTransaction = transactionDao.findTransactionByExternalId(transactionEntity.getExternalId()).get();

        assertThat(retrievedTransaction.getSource(), is(Source.CARD_API));
    }

    @Test
    void shouldRetrieveTransactionByExternalIdAndGatewayAccount() {
        ZonedDateTime paidOutDate = ZonedDateTime.parse("2019-12-12T10:00:00Z");
        String payOutId = randomAlphanumeric(20);

        TransactionFixture fixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withExternalMetadata(ImmutableMap.of("key1", "value1", "anotherKey", ImmutableMap.of("nestedKey", "value")))
                .withDefaultTransactionDetails()
                .withGatewayPayoutId(payOutId)
                .insert(rule.getJdbi());
        aPayoutFixture()
                .withGatewayAccountId(fixture.getGatewayAccountId())
                .withGatewayPayoutId(payOutId)
                .withPaidOutDate(paidOutDate)
                .build()
                .insert(rule.getJdbi());

        TransactionEntity retrievedTransaction = transactionDao.findTransactionByExternalIdAndGatewayAccountId(
                fixture.getExternalId(), fixture.getGatewayAccountId()).get();

        assertTransactionEntity(retrievedTransaction, fixture);
        assertThat(retrievedTransaction.getFee(), is(nullValue()));
        assertThat(retrievedTransaction.getTotalAmount(), is(nullValue()));
        assertThat(retrievedTransaction.getNetAmount(), is(nullValue()));
        assertThat(retrievedTransaction.getPayoutEntity().isPresent(), is(true));
        assertThat(retrievedTransaction.getPayoutEntity().get().getPaidOutDate(), is(paidOutDate));
    }

    @Test
    void shouldRetrieveTransactionWithPayoutDateByExternalIdAndNoGatewayAccount() {
        ZonedDateTime paidOutDate = ZonedDateTime.parse("2019-12-12T10:00:00Z");
        String payOutId = randomAlphanumeric(20);

        TransactionFixture fixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withExternalMetadata(ImmutableMap.of("key1", "value1", "anotherKey", ImmutableMap.of("nestedKey", "value")))
                .withDefaultTransactionDetails()
                .withGatewayPayoutId(payOutId)
                .insert(rule.getJdbi());
        aPayoutFixture()
                .withGatewayAccountId(fixture.getGatewayAccountId())
                .withGatewayPayoutId(payOutId)
                .withPaidOutDate(paidOutDate)
                .build()
                .insert(rule.getJdbi());

        TransactionEntity retrievedTransaction = transactionDao.findTransactionByExternalId(fixture.getExternalId()).get();
        
        assertThat(retrievedTransaction.getPayoutEntity().isPresent(), is(true));
        assertThat(retrievedTransaction.getPayoutEntity().get().getPaidOutDate(), is(paidOutDate));
    }

    private void assertTransactionEntity(TransactionEntity transaction, TransactionFixture fixture) {
        assertThat(transaction.getId(), notNullValue());
        assertThat(transaction.getGatewayAccountId(), is(fixture.getGatewayAccountId()));
        assertThat(transaction.getExternalId(), is(fixture.getExternalId()));
        assertThat(transaction.getAmount(), is(fixture.getAmount()));
        assertThat(transaction.getReference(), is(fixture.getReference()));
        assertThat(transaction.getDescription(), is(fixture.getDescription()));
        assertThat(transaction.getState(), is(fixture.getState()));
        assertThat(transaction.getEmail(), is(fixture.getEmail()));
        assertThat(transaction.getCardholderName(), is(fixture.getCardholderName()));
        assertThat(transaction.getTransactionDetails(), containsString("\"external_metadata\": {\"key1\": \"value1\", \"anotherKey\": {\"nestedKey\": \"value\"}}"));
        assertThat(transaction.getCreatedDate(), is(fixture.getCreatedDate()));
        assertThat(transaction.getTransactionDetails().contains(fixture.getLanguage()), is(true));
        assertThat(transaction.getTransactionDetails().contains(fixture.getReturnUrl()), is(true));
        assertThat(transaction.getTransactionDetails().contains(fixture.getPaymentProvider()), is(true));
        assertThat(transaction.getTransactionDetails().contains(fixture.getCardDetails().getBillingAddress().getAddressLine1()), is(true));
        assertThat(transaction.getEventCount(), is(fixture.getEventCount()));
        assertThat(transaction.getCardBrand(), is(fixture.getCardBrand()));
        assertThat(transaction.getLastDigitsCardNumber(), is(fixture.getLastDigitsCardNumber()));
        assertThat(transaction.getFirstDigitsCardNumber(), is(fixture.getFirstDigitsCardNumber()));
        assertThat(transaction.getTransactionType(), is(fixture.getTransactionType()));
        assertThat(transaction.getRefundAmountAvailable(), is(fixture.getRefundAmountAvailable()));
        assertThat(transaction.getRefundAmountRefunded(), is(fixture.getRefundAmountRefunded()));
        assertThat(transaction.getRefundStatus(), is(fixture.getRefundStatus()));
        assertThat(transaction.getGatewayTransactionId(), is(fixture.getGatewayTransactionId()));
        assertThat(transaction.getGatewayPayoutId(), is(fixture.getGatewayPayoutId()));
    }

    @Test
    void shouldRetrieveTransactionByExternalIdAndGatewayAccountId() {
        String payOutId = randomAlphanumeric(20);
        ZonedDateTime paidOutDate = ZonedDateTime.parse("2019-12-12T10:00:00Z");

        TransactionFixture fixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withTransactionType("PAYMENT")
                .withExternalMetadata(ImmutableMap.of("key1", "value1", "anotherKey", ImmutableMap.of("nestedKey", "value")))
                .withDefaultTransactionDetails()
                .withGatewayPayoutId(payOutId)
                .insert(rule.getJdbi());

        aPayoutFixture()
                .withPaidOutDate(paidOutDate)
                .withGatewayPayoutId(payOutId)
                .withGatewayAccountId(fixture.getGatewayAccountId())
                .build()
                .insert(rule.getJdbi());

        TransactionEntity transaction = transactionDao
                .findTransaction(
                        fixture.getExternalId(),
                        fixture.getGatewayAccountId(),
                        TransactionType.PAYMENT, null).get();

        assertTransactionEntity(transaction, fixture);
        assertThat(transaction.getFee(), is(nullValue()));
        assertThat(transaction.getTotalAmount(), is(nullValue()));
        assertThat(transaction.getPayoutEntity().isPresent(), is(true));
        assertThat(transaction.getPayoutEntity().get().getPaidOutDate(), is(paidOutDate));
    }

    @Test
    void shouldRetrieveRefundTransactionByExternalIdAndGatewayAccountId() {
        TransactionEntity parentTransactionEntity = aTransactionFixture()
                .withTransactionType(TransactionType.PAYMENT.name())
                .withDefaultTransactionDetails()
                .withGatewayPayoutId("payment-payout-id")
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity transactionEntity = aTransactionFixture()
                .withTransactionType(TransactionType.REFUND.name())
                .withParentExternalId(parentTransactionEntity.getExternalId())
                .withDefaultTransactionDetails()
                .withGatewayPayoutId("refund-payout-id")
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity transaction = transactionDao
                .findTransaction(
                        transactionEntity.getExternalId(),
                        transactionEntity.getGatewayAccountId(),
                        TransactionType.REFUND, parentTransactionEntity.getExternalId())
                .get();

        assertThat(transaction.getId(), notNullValue());
        assertThat(transaction.getGatewayAccountId(), is(transactionEntity.getGatewayAccountId()));
        assertThat(transaction.getExternalId(), is(transactionEntity.getExternalId()));
        assertThat(transaction.getAmount(), is(transactionEntity.getAmount()));
        assertThat(transaction.getDescription(), is(transactionEntity.getDescription()));
        assertThat(transaction.getState(), is(transactionEntity.getState()));
        assertThat(transaction.getCreatedDate(), is(transactionEntity.getCreatedDate()));
        assertThat(transaction.getEventCount(), is(transactionEntity.getEventCount()));
        assertThat(transaction.getTransactionType(), is(transactionEntity.getTransactionType()));
        assertThat(transaction.getGatewayPayoutId(), is(transactionEntity.getGatewayPayoutId()));
    }

    @Test
    void shouldUpsertTransaction() {
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
    void shouldNotOverwriteTransactionIfItConsistsOfFewerEvents() {
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
    void shouldFilterTransactionByExternalIdOrParentExternalIdAndGatewayAccountId() {
        String payOutId = randomAlphanumeric(20);
        ZonedDateTime paidOutDate = ZonedDateTime.parse("2019-12-12T10:00:00Z");

        TransactionEntity transaction1 = aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withExternalId("external-id-1")
                .withGatewayAccountId("gateway-account-id-1")
                .withGatewayPayoutId(payOutId)
                .insert(rule.getJdbi())
                .toEntity();

        aTransactionFixture()
                .withState(TransactionState.SUBMITTED)
                .withExternalId("external-id-2")
                .withGatewayAccountId("gateway-account-id-2")
                .insert(rule.getJdbi());

        aPayoutFixture()
                .withPaidOutDate(paidOutDate)
                .withGatewayPayoutId(payOutId)
                .withGatewayAccountId(transaction1.getGatewayAccountId())
                .build()
                .insert(rule.getJdbi());

        List<TransactionEntity> transactionEntityList =
                transactionDao.findTransactionByExternalOrParentIdAndGatewayAccountId(
                        transaction1.getExternalId(), transaction1.getGatewayAccountId());

        assertThat(transactionEntityList.size(), is(1));
        assertThat(transactionEntityList.get(0).getExternalId(), is(transaction1.getExternalId()));
        assertThat(transactionEntityList.get(0).getGatewayAccountId(), is(transaction1.getGatewayAccountId()));
        assertThat(transactionEntityList.get(0).getPayoutEntity().isPresent(), is(true));
        assertThat(transactionEntityList.get(0).getPayoutEntity().get().getPaidOutDate(), is(paidOutDate));
    }

    @Test
    void findTransactionByExternalOrParentIdAndGatewayAccountId_shouldFilterByParentExternalId() {
        TransactionEntity transaction1 = aTransactionFixture()
                .withState(TransactionState.CREATED)
                .insert(rule.getJdbi())
                .toEntity();

        aTransactionFixture()
                .withState(TransactionState.SUBMITTED)
                .withGatewayAccountId(transaction1.getGatewayAccountId())
                .withParentExternalId(transaction1.getExternalId())
                .insert(rule.getJdbi());

        List<TransactionEntity> transactionEntityList =
                transactionDao.findTransactionByExternalOrParentIdAndGatewayAccountId(
                        transaction1.getExternalId(), transaction1.getGatewayAccountId());

        assertThat(transactionEntityList.size(), is(2));
    }

    @Test
    void findTransactionByParentIdAndGatewayAccountId_shouldFilterByParentExternalId() {
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

    @Test
    void findTransactionByParentId_shouldFilterByParentExternalId() {
        String payOutId = randomAlphanumeric(20);
        ZonedDateTime paidOutDate = ZonedDateTime.parse("2019-12-12T10:00:00Z");

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
                .withGatewayPayoutId(payOutId)
                .insert(rule.getJdbi())
                .toEntity();

        aPayoutFixture()
                .withPaidOutDate(paidOutDate)
                .withGatewayPayoutId(payOutId)
                .withGatewayAccountId(transaction1.getGatewayAccountId())
                .build()
                .insert(rule.getJdbi());

        List<TransactionEntity> transactionEntityList =
                transactionDao.findTransactionByParentId(transactionWithParentExternalId.getParentExternalId());

        assertThat(transactionEntityList.size(), is(1));
        TransactionEntity transactionEntity = transactionEntityList.get(0);

        assertThat(transactionEntity.getId(), is(transactionWithParentExternalId.getId()));
        assertThat(transactionEntity.getPayoutEntity().isPresent(), is(true));
        assertThat(transactionEntity.getPayoutEntity().get().getPaidOutDate(), is(paidOutDate));
    }

    @Test
    void sourceTypeInDatabase_shouldMatchValuesInEnum() {
        var sourceArray = Arrays.stream(Source.values()).map(Enum::toString).collect(Collectors.toList());
        transactionDao.getSourceTypeValues().forEach(x -> assertThat(sourceArray.contains(x), is(true)));
        sourceArray.forEach(x -> assertThat(transactionDao.getSourceTypeValues().contains(x), is(true)));
    }
}
