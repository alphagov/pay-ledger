package uk.gov.pay.ledger.transactionmetadata.dao;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.metadatakey.dao.MetadataKeyDao;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import java.util.List;
import java.util.Map;

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

    @Before
    public void setUp() {
        transactionMetadataDao = rule.getJdbi().onDemand(TransactionMetadataDao.class);
        metadataKeyDao = rule.getJdbi().onDemand(MetadataKeyDao.class);
        dbHelper = aDatabaseTestHelper(rule.getJdbi());

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
}
