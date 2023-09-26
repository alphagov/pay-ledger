package uk.gov.pay.ledger.transaction.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;
import uk.gov.service.payments.commons.model.Source;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.parse;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static uk.gov.pay.ledger.transaction.service.TransactionService.REDACTED_REFERENCE_NUMBER;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.PayoutFixture.PayoutFixtureBuilder.aPayoutFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

class TransactionDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private TransactionDao transactionDao = new TransactionDao(rule.getJdbi(), mock(LedgerConfig.class));
    private ObjectMapper objectMapper = new ObjectMapper();

    private DatabaseTestHelper databaseTestHelper = aDatabaseTestHelper(rule.getJdbi());

    @BeforeEach
    public void setUp() {
        databaseTestHelper.truncateAllData();
    }

    @Test
    void shouldInsertTransaction() {
        TransactionFixture fixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withNetAmount(55)
                .withTotalAmount(105)
                .withFee(33L)
                .withExternalMetadata(ImmutableMap.of("key1", "value1", "anotherKey", ImmutableMap.of("nestedKey", "value")))
                .withTransactionType("PAYMENT")
                .withLive(true)
                .withGatewayTransactionId("gateway_transaction_id")
                .withGatewayPayoutId("payout-id")
                .withAgreementId("an-agreement-id")
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
        ZonedDateTime paidOutDate = parse("2019-12-12T10:00:00Z");
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
        ZonedDateTime paidOutDate = parse("2019-12-12T10:00:00Z");
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
        assertThat(transaction.getServiceId(), is(fixture.getServiceId()));
        assertThat(transaction.isLive(), is(fixture.isLive()));
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
        assertThat(transaction.getAgreementId(), is(fixture.getAgreementId()));
    }

    @Test
    void shouldRetrieveTransactionByExternalIdAndGatewayAccountId() {
        String payOutId = randomAlphanumeric(20);
        ZonedDateTime paidOutDate = parse("2019-12-12T10:00:00Z");

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
                        null, parentTransactionEntity.getExternalId())
                .get();

        assertThat(transaction.getId(), notNullValue());
        assertThat(transaction.getServiceId(), is(transactionEntity.getServiceId()));
        assertThat(transaction.isLive(), is(transactionEntity.isLive()));
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
                .withReference("4242424242424242")
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity modifiedTransaction = aTransactionFixture()
                .withExternalId(transaction.getExternalId())
                .withEventCount(2)
                .withState(TransactionState.SUBMITTED)
                .withReference(REDACTED_REFERENCE_NUMBER)
                .toEntity();

        transactionDao.upsert(modifiedTransaction);

        TransactionEntity retrievedTransaction = transactionDao.findTransactionByExternalId(transaction.getExternalId()).get();

        assertThat(retrievedTransaction.getState(), is(modifiedTransaction.getState()));
        assertThat(retrievedTransaction.getReference(), is(REDACTED_REFERENCE_NUMBER));
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
        ZonedDateTime paidOutDate = parse("2019-12-12T10:00:00Z");

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
    void findTransactionByParentIdAndGatewayAccountId_shouldFilterByParentExternalId_whenNoTransactionTypeSpecified() {
        TransactionEntity transaction1 = aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withTransactionType("PAYMENT")
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity refundTransaction = aTransactionFixture()
                .withTransactionType(TransactionType.REFUND.name())
                .withGatewayAccountId(transaction1.getGatewayAccountId())
                .withParentExternalId(transaction1.getExternalId())
                .insert(rule.getJdbi())
                .toEntity();
        TransactionEntity disputeTransaction = aTransactionFixture()
                .withTransactionType(TransactionType.DISPUTE.name())
                .withGatewayAccountId(transaction1.getGatewayAccountId())
                .withParentExternalId(transaction1.getExternalId())
                .insert(rule.getJdbi())
                .toEntity();

        List<TransactionEntity> transactionEntityList =
                transactionDao.findTransactionsByParentIdAndGatewayAccountId(
                        transaction1.getExternalId(),
                        transaction1.getGatewayAccountId(),
                        null);

        assertThat(transactionEntityList.size(), is(2));
        assertThat(transactionEntityList, containsInAnyOrder(
                hasProperty("id", is(refundTransaction.getId())),
                hasProperty("id", is(disputeTransaction.getId()))
        ));
    }

    @Test
    void findTransactionByParentIdAndGatewayAccountId_shouldFilterByParentExternalIdAndTransactionType() {
        TransactionEntity transaction1 = aTransactionFixture()
                .withTransactionType(TransactionType.PAYMENT.name())
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity refundTransaction = aTransactionFixture()
                .withTransactionType(TransactionType.REFUND.name())
                .withGatewayAccountId(transaction1.getGatewayAccountId())
                .withParentExternalId(transaction1.getExternalId())
                .insert(rule.getJdbi())
                .toEntity();
        aTransactionFixture()
                .withTransactionType(TransactionType.DISPUTE.name())
                .withGatewayAccountId(transaction1.getGatewayAccountId())
                .withParentExternalId(transaction1.getExternalId())
                .insert(rule.getJdbi());

        List<TransactionEntity> transactionEntityList =
                transactionDao.findTransactionsByParentIdAndGatewayAccountId(
                        transaction1.getExternalId(),
                        transaction1.getGatewayAccountId(),
                        TransactionType.REFUND);

        assertThat(transactionEntityList.size(), is(1));
        TransactionEntity transactionEntity = transactionEntityList.get(0);

        assertThat(transactionEntity.getId(), is(refundTransaction.getId()));
    }

    @Test
    void findTransactionByParentId_shouldFilterByParentExternalId() {
        String payOutId = randomAlphanumeric(20);
        ZonedDateTime paidOutDate = parse("2019-12-12T10:00:00Z");

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

    @Nested
    @DisplayName("FindTransactionsForRedaction")
    class TestTransactionsForRedaction {

        @Test
        void shouldReturnTransactionsCorrectlyForDateRangesAndNumberOfTransactions() {
            TransactionEntity transactionToReturnToDelete1 = aTransactionFixture()
                    .withCreatedDate(parse("2016-01-01T00:00:00Z"))
                    .insert(rule.getJdbi())
                    .toEntity();
            TransactionEntity transactionToReturnToDelete2 = aTransactionFixture()
                    .withCreatedDate(parse("2016-01-01T01:00:00Z"))
                    .insert(rule.getJdbi())
                    .toEntity();
            TransactionEntity transactionEligibleForSearchButNotReturnedDueToLimit = aTransactionFixture()
                    .withCreatedDate(parse("2016-01-01T02:00:00Z"))
                    .insert(rule.getJdbi())
                    .toEntity();

            TransactionEntity transactionToExclude1 = aTransactionFixture()
                    .withCreatedDate(parse("2016-01-02T00:00:00Z"))
                    .insert(rule.getJdbi())
                    .toEntity();
            TransactionEntity transactionToExclude2 = aTransactionFixture()
                    .withCreatedDate(parse("2016-01-03T00:00:00Z"))
                    .insert(rule.getJdbi())
                    .toEntity();

            List<TransactionEntity> transactionsForRedaction = transactionDao.findTransactionsForRedaction(
                    parse("2015-12-31T00:00:00Z"),
                    parse("2016-01-02T00:00:00Z"),
                    2
            );

            List<String> transactionIdsReturned = transactionsForRedaction
                    .stream()
                    .map(TransactionEntity::getExternalId)
                    .collect(Collectors.toList());

            assertThat(transactionsForRedaction.size(), is(2));
            assertThat(transactionIdsReturned, hasItems(
                    transactionToReturnToDelete1.getExternalId(),
                    transactionToReturnToDelete2.getExternalId())
            );
            assertThat(transactionIdsReturned, not(hasItems(
                    transactionEligibleForSearchButNotReturnedDueToLimit.getExternalId(),
                    transactionToExclude1.getExternalId(),
                    transactionToExclude2.getExternalId())
            ));
        }
    }

    @Nested
    @DisplayName("RedactPIIFromTransaction")
    class TestRedactPIIFromTransaction {

        @Test
        void shouldRedactAllPIIFromTransactionCorrectly() throws JsonProcessingException {

            JsonObject transactionDetails1 = new JsonObject();
            transactionDetails1.addProperty("reference", "ref-1");
            transactionDetails1.addProperty("cardholder_name", "Joe B");
            transactionDetails1.addProperty("email", "test@email.com");
            transactionDetails1.addProperty("address_line1", "line 1");
            transactionDetails1.addProperty("address_line2", "line 2");

            TransactionEntity transactionToRedact = aTransactionFixture()
                    .withReference("ref-1")
                    .withEmail("test@email.com")
                    .withCardholderName("Joe B")
                    .withTransactionDetails(transactionDetails1.toString())
                    .insert(rule.getJdbi())
                    .toEntity();

            JsonObject transactionDetails2 = new JsonObject();
            transactionDetails2.addProperty("reference", "ref-2");
            transactionDetails2.addProperty("cardholder_name", "Jane D");
            transactionDetails2.addProperty("email", "test2@email.com");
            TransactionEntity transactionThatShouldNotBeRedacted = aTransactionFixture()
                    .withReference("ref-2")
                    .withEmail("test2@email.com")
                    .withCardholderName("Jane D")
                    .withTransactionDetails(transactionDetails2.toString())
                    .insert(rule.getJdbi())
                    .toEntity();

            transactionDao.redactPIIFromTransaction(transactionToRedact.getExternalId());

            TransactionEntity transactionEntityRedacted = transactionDao.findTransactionByExternalId(transactionToRedact.getExternalId()).get();

            assertThat(transactionEntityRedacted.getReference(), is("<REDACTED>"));
            assertThat(transactionEntityRedacted.getCardholderName(), is("<REDACTED>"));
            assertThat(transactionEntityRedacted.getEmail(), is("<REDACTED>"));

            JsonNode jsonNode = objectMapper.readTree(transactionEntityRedacted.getTransactionDetails());

            assertThat(jsonNode.get("reference"), is(Matchers.nullValue()));
            assertThat(jsonNode.get("cardholder_name"), is(nullValue()));
            assertThat(jsonNode.get("email"), is(nullValue()));
            assertThat(jsonNode.get("address_line1").asText(), is("<REDACTED>"));
            assertThat(jsonNode.get("address_line2").asText(), is("<REDACTED>"));

            TransactionEntity transactionEntityNotRedacted = transactionDao.findTransactionByExternalId(transactionThatShouldNotBeRedacted.getExternalId()).get();
            assertThat(transactionEntityNotRedacted.getReference(), is("ref-2"));
            assertThat(transactionEntityNotRedacted.getCardholderName(), is("Jane D"));
            assertThat(transactionEntityNotRedacted.getEmail(), is("test2@email.com"));
        }

        @Test
        void shouldNotReplaceNonMandatoryFieldsWithARedactedStringWhenBlank() throws JsonProcessingException {
            JsonObject transactionDetails1 = new JsonObject();
            transactionDetails1.addProperty("reference", "ref-1");

            TransactionEntity transactionToRedact = aTransactionFixture()
                    .withReference("ref-1")
                    .withEmail(null)
                    .withCardholderName(null)
                    .withTransactionDetails(transactionDetails1.toString())
                    .insert(rule.getJdbi())
                    .toEntity();

            transactionDao.redactPIIFromTransaction(transactionToRedact.getExternalId());

            TransactionEntity transactionEntity = transactionDao.findTransactionByExternalId(transactionToRedact.getExternalId()).get();

            assertThat(transactionEntity.getReference(), is("<REDACTED>"));
            assertThat(transactionEntity.getCardholderName(), is(nullValue()));
            assertThat(transactionEntity.getEmail(), is(nullValue()));

            JsonNode jsonNode = objectMapper.readTree(transactionEntity.getTransactionDetails());

            assertThat(jsonNode.get("reference"), is(nullValue()));
            assertThat(jsonNode.get("cardholder_name"), is(nullValue()));
            assertThat(jsonNode.get("email"), is(nullValue()));
            assertThat(jsonNode.get("address_line1"), is(nullValue()));
            assertThat(jsonNode.get("address_line2"), is(nullValue()));
        }
    }

    @Nested
    @DisplayName("CreatedDateOfFirstTransaction")
    class TestCreatedDateOfFirstTransaction {

        @Test
        void shouldReturnTheDateOfFirstTransactionCorrectly() {
            aTransactionFixture().withCreatedDate(parse("2016-01-02T00:00:01Z"))
                    .insert(rule.getJdbi());
            aTransactionFixture().withCreatedDate(parse("2019-01-03T00:00:00Z"))
                    .insert(rule.getJdbi());
            aTransactionFixture().withCreatedDate(parse("2023-01-03T00:00:00Z"))
                    .insert(rule.getJdbi());

            Optional<ZonedDateTime> mayBeCreatedDateOfFirstTransaction = transactionDao.getCreatedDateOfFirstTransaction();

            assertThat(mayBeCreatedDateOfFirstTransaction.isPresent(), is(true));
            assertThat(mayBeCreatedDateOfFirstTransaction.get().withZoneSameInstant(UTC).toString(), is("2016-01-02T00:00:01Z"));
        }

        @Test
        void shouldReturnEmptyOptionalIfThereAreNoTransactions() {
            Optional<ZonedDateTime> mayBeCreatedDateOfFirstTransaction = transactionDao.getCreatedDateOfFirstTransaction();

            assertThat(mayBeCreatedDateOfFirstTransaction.isPresent(), is(false));
        }
    }
}
