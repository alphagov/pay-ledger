package uk.gov.pay.ledger.metadatakey.dao;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;

public class TransactionMetadataKeyDaoIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private MetadataKeyDao metadataKeyDao;
    private DatabaseTestHelper dbHelper;

    @Before
    public void setUp(){
        metadataKeyDao = rule.getJdbi().onDemand(MetadataKeyDao.class);
        dbHelper = aDatabaseTestHelper(rule.getJdbi());
    }

    @Test
    public void shouldInsertMetadataKey() {
        String key = "key-1";

        metadataKeyDao.insertIfNotExist(key);

        List<Map<String, Object>> metadataKeyRecord = dbHelper.getMetadataKey(key);

        assertThat(metadataKeyRecord.size(), is(1));
        assertThat(metadataKeyRecord.get(0).get("key"), is(key));
    }

    @Test
    public void shouldNotInsertMetadataKeyIfAlreadyExists() {
        String key = "key-2";
        String duplicateKey = "key-1";

        metadataKeyDao.insertIfNotExist(key);
        metadataKeyDao.insertIfNotExist(duplicateKey);

        List<Map<String, Object>> metadataKeyRecord = dbHelper.getMetadataKey(key);

        assertThat(metadataKeyRecord.size(), is(1));
        assertThat(metadataKeyRecord.get(0).get("key"), is(key));
    }
}
