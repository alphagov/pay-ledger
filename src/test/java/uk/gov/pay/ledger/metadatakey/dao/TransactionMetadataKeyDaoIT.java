package uk.gov.pay.ledger.metadatakey.dao;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;

public class TransactionMetadataKeyDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private MetadataKeyDao metadataKeyDao;
    private DatabaseTestHelper dbHelper;

    @BeforeEach
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
