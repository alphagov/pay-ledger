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
    private TransactionMetadataDao transactionMetadataDao;
    private DatabaseTestHelper dbHelper;
    private MetadataKeyDao metadataKeyDao;

    @BeforeEach
    public void setUp() {
        transactionMetadataDao = new TransactionMetadataDao(rule.getJdbi());
        metadataKeyDao = rule.getJdbi().onDemand(MetadataKeyDao.class);
        dbHelper = aDatabaseTestHelper(rule.getJdbi());
        String key = "key-1";
        metadataKeyDao.insertIfNotExist(key);
    }

    @Test
    public void shouldUpsertValueWhenExisting() {
        String gatewayAccountId = randomAlphanumeric(15);
        String reference = randomAlphanumeric(15);
        TransactionEntity transaction = insertTransaction(gatewayAccountId, reference,
                ImmutableMap.of("test-key-1", "value1"));
        metadataKeyDao.insertIfNotExist("test-key-1");
        aTransactionMetadataFixture().withTransactionId(transaction.getId())
                .withMetadataKey("test-key-1").withValue("value1").insert(rule.getJdbi());

        transactionMetadataDao.upsert(transaction.getId(), "test-key-1", "value3");

        List<Map<String, Object>> transactionMetadata = dbHelper.getTransactionMetadata(transaction.getId(), "test-key-1");

        assertThat(transactionMetadata.size(), is(1));
        assertThat(transactionMetadata.get(0).get("transaction_id"), is(transaction.getId()));
        assertThat(transactionMetadata.get(0).get("metadata_key_id"), is(notNullValue()));
        assertThat(transactionMetadata.get(0).get("value"), is("value3"));
    }

    @Test
    public void shouldInsertWhenNotExisting() {
        String gatewayAccountId = randomAlphanumeric(15);
        String reference = randomAlphanumeric(15);
        TransactionEntity transaction = insertTransaction(gatewayAccountId, reference,
                ImmutableMap.of("test-key-2", "value2"));

        metadataKeyDao.insertIfNotExist("test-key-2");

        transactionMetadataDao.upsert(transaction.getId(), "test-key-2", "value2");

        List<Map<String, Object>> transactionMetadata = dbHelper.getTransactionMetadata(transaction.getId(), "test-key-2");

        assertThat(transactionMetadata.get(0).get("value"), is("value2"));
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