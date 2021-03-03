package uk.gov.pay.ledger.transactionmetadata.dao;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.metadatakey.dao.MetadataKeyDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionMetadataFixture.aTransactionMetadataFixture;

public class TransactionMetadataDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();
    private static String key = "key-1";
    private TransactionMetadataDao transactionMetadataDao;
    private DatabaseTestHelper dbHelper;
    private MetadataKeyDao metadataKeyDao;
    private TransactionFixture transactionFixture;
    private TransactionSearchParams searchParams;

    @BeforeEach
    public void setUp() {
        transactionMetadataDao = new TransactionMetadataDao(rule.getJdbi());
        metadataKeyDao = rule.getJdbi().onDemand(MetadataKeyDao.class);
        dbHelper = aDatabaseTestHelper(rule.getJdbi());

        searchParams = new TransactionSearchParams();
        metadataKeyDao.insertIfNotExist(key);
        transactionFixture = aTransactionFixture()
                .insert(rule.getJdbi());
    }

    @Test
    public void shouldInsertTransactionMetadataForAGivenTransactionIdAndMetadataKey() {
        transactionMetadataDao.insertIfNotExist(transactionFixture.getId(), key);

        List<Map<String, Object>> transactionMetadata = dbHelper.getTransactionMetadata(transactionFixture.getId(), key);

        assertThat(transactionMetadata.size(), is(1));
        assertThat(transactionMetadata.get(0).get("transaction_id"), is(transactionFixture.getId()));
        assertThat(transactionMetadata.get(0).get("metadata_key_id"), is(notNullValue()));
    }

    @Test
    public void shouldNotInsertTransactionMetadataForTransactionAndMetadataKeyIfAlreadyExists() {
        String duplicateKey = "key-1";

        transactionMetadataDao.insertIfNotExist(transactionFixture.getId(), key);
        transactionMetadataDao.insertIfNotExist(transactionFixture.getId(), duplicateKey);

        List<Map<String, Object>> transactionMetadata = dbHelper.getTransactionMetadata(transactionFixture.getId(), key);

        assertThat(transactionMetadata.size(), is(1));
        assertThat(transactionMetadata.get(0).get("transaction_id"), is(transactionFixture.getId()));
        assertThat(transactionMetadata.get(0).get("metadata_key_id"), is(notNullValue()));
    }

    @Test
    public void shouldReturnCorrectMetadataKeysForTransactionSearch() {
        String gatewayAccountId = randomAlphanumeric(15);
        String reference = randomAlphanumeric(15);
        TransactionEntity transaction1 = insertTransaction(gatewayAccountId, reference,
                ImmutableMap.of("test-key-1", "value1"));
        TransactionEntity transaction2 = insertTransaction(gatewayAccountId, reference,
                ImmutableMap.of("test-key-2", "value1"));
        TransactionEntity transactionToBeExcluded = insertTransaction(gatewayAccountId, "ref123",
                ImmutableMap.of("test-key-3", "value1"));

        metadataKeyDao.insertIfNotExist("test-key-1");
        metadataKeyDao.insertIfNotExist("test-key-2");
        metadataKeyDao.insertIfNotExist("test-key-3");

        transactionMetadataDao.insertIfNotExist(transaction1.getId(), "test-key-1");
        transactionMetadataDao.insertIfNotExist(transaction2.getId(), "test-key-2");

        searchParams.setAccountIds(List.of(transaction1.getGatewayAccountId()));
        searchParams.setFromDate(ZonedDateTime.now().minusDays(10).toString());
        searchParams.setToDate(ZonedDateTime.now().plusDays(1).toString());
        searchParams.setFirstDigitsCardNumber(transaction1.getFirstDigitsCardNumber());
        searchParams.setLastDigitsCardNumber(transaction1.getLastDigitsCardNumber());
        searchParams.setCardHolderName(transaction1.getCardholderName());
        searchParams.setReference(transaction1.getReference());
        searchParams.setEmail(transaction1.getEmail());
        List<String> transactionEntityList =
                transactionMetadataDao.findMetadataKeysForTransactions(searchParams);

        MatcherAssert.assertThat(transactionEntityList.size(), is(2));

        assertThat(transactionEntityList, hasItem("test-key-1"));
        assertThat(transactionEntityList, hasItem("test-key-2"));
    }

    @Test
    public void shouldReturnCorrectMetadataKeysWhenTransactionsAreSearchedByMetadataValue() {
        String gatewayAccountId = randomAlphanumeric(15);
        String reference = randomAlphanumeric(15);
        TransactionEntity transaction1 = insertTransaction(gatewayAccountId, reference,
                ImmutableMap.of("test-key-1", "value1", "test-key-2", "value2"));
        TransactionEntity transaction2 = insertTransaction(gatewayAccountId, reference,
                ImmutableMap.of("test-key-n", "value1"));
        TransactionEntity transactionToBeExcluded = insertTransaction(gatewayAccountId, "ref123",
                ImmutableMap.of("test-key-3", "value3"));

        metadataKeyDao.insertIfNotExist("test-key-1");
        metadataKeyDao.insertIfNotExist("test-key-2");
        metadataKeyDao.insertIfNotExist("test-key-3");
        metadataKeyDao.insertIfNotExist("test-key-n");

        //todo: replace with transactionMetadataDao.insertIfNotExist() when this supports inserting 'value'
        aTransactionMetadataFixture().withTransactionId(transaction1.getId())
                .withMetadataKey("test-key-1").withValue("value1").insert(rule.getJdbi());
        aTransactionMetadataFixture().withTransactionId(transaction1.getId())
                .withMetadataKey("test-key-2").withValue("value3").insert(rule.getJdbi());
        aTransactionMetadataFixture().withTransactionId(transaction2.getId())
                .withMetadataKey("test-key-n").withValue("value1").insert(rule.getJdbi());
        aTransactionMetadataFixture().withTransactionId(transactionToBeExcluded.getId())
                .withMetadataKey("test-key-3").withValue("value3").insert(rule.getJdbi());

        searchParams.setAccountIds(List.of(transaction1.getGatewayAccountId()));
        searchParams.setFromDate(ZonedDateTime.now().minusDays(10).toString());
        searchParams.setToDate(ZonedDateTime.now().plusDays(1).toString());
        searchParams.setFirstDigitsCardNumber(transaction1.getFirstDigitsCardNumber());
        searchParams.setLastDigitsCardNumber(transaction1.getLastDigitsCardNumber());
        searchParams.setCardHolderName(transaction1.getCardholderName());
        searchParams.setReference(transaction1.getReference());
        searchParams.setEmail(transaction1.getEmail());

        searchParams.setMetadataValue("value1");

        List<String> transactionEntityList =
                transactionMetadataDao.findMetadataKeysForTransactions(searchParams);

        MatcherAssert.assertThat(transactionEntityList.size(), is(3));

        assertThat(transactionEntityList, hasItem("test-key-1"));
        assertThat(transactionEntityList, hasItem("test-key-n"));

        // value for this metadata key is 'value2' but key expected in the list (for CSV).
        // This is because corresponding transaction has matching metadata value on another key 'test-key-1'.
        assertThat(transactionEntityList, hasItem("test-key-2"));
    }

    private TransactionEntity insertTransaction(String gatewayAccountId, String reference, Map<String, Object> externalMetadata) {
        return aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withTransactionType("PAYMENT")
                .withGatewayAccountId(gatewayAccountId)
                .withReference(reference)
                .withExternalMetadata(externalMetadata)
                .withDefaultCardDetails()
                .withFirstDigitsCardNumber("123456")
                .withLastDigitsCardNumber("1234")
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi())
                .toEntity();
    }
}