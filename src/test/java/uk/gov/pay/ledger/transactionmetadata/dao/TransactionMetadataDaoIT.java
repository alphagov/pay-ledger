package uk.gov.pay.ledger.transactionmetadata.dao;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.metadatakey.dao.MetadataKeyDao;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class TransactionMetadataDaoIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();
    private static String key = "key-1";
    private TransactionMetadataDao transactionMetadataDao;
    private DatabaseTestHelper dbHelper;
    private MetadataKeyDao metadataKeyDao;
    private TransactionFixture transactionFixture;
    private TransactionSearchParams searchParams;

    @Before
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
        TransactionEntity transaction1 = aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withTransactionType("PAYMENT")
                .withExternalMetadata(ImmutableMap.of("test-key-1", "value1"))
                .withDefaultCardDetails()
                .withFirstDigitsCardNumber("123456")
                .withLastDigitsCardNumber("1234")
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity transaction2 = aTransactionFixture()
                .withState(TransactionState.SUBMITTED)
                .withReference(transaction1.getReference())
                .withGatewayAccountId(transaction1.getGatewayAccountId())
                .withExternalMetadata(ImmutableMap.of("test-key-2", "value1"))
                .withDefaultCardDetails()
                .withFirstDigitsCardNumber("123456")
                .withLastDigitsCardNumber("1234")
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi())
                .toEntity();

        TransactionEntity transactionToBeExcluded = aTransactionFixture()
                .withState(TransactionState.SUBMITTED)
                .withCreatedDate(ZonedDateTime.now().plusDays(3))
                .withGatewayAccountId(transaction1.getGatewayAccountId())
                .withExternalMetadata(ImmutableMap.of("test-key-3", "value1"))
                .withDefaultTransactionDetails()
                .insert(rule.getJdbi())
                .toEntity();

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
}