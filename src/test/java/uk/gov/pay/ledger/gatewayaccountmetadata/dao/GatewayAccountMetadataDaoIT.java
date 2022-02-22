package uk.gov.pay.ledger.gatewayaccountmetadata.dao;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.metadatakey.dao.MetadataKeyDao;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.GatewayAccountMetadataFixture.aGatewayAccountMetadataFixture;

class GatewayAccountMetadataDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();
    private GatewayAccountMetadataDao gatewayAccountMetadataDao;
    private DatabaseTestHelper dbHelper;
    private MetadataKeyDao metadataKeyDao;

    @BeforeEach
    public void setUp() {
        gatewayAccountMetadataDao = new GatewayAccountMetadataDao(rule.getJdbi());
        metadataKeyDao = rule.getJdbi().onDemand(MetadataKeyDao.class);
        dbHelper = aDatabaseTestHelper(rule.getJdbi());
    }

    @Test
    public void shouldReturnCorrectMetadataKeysForGatewayAccount() {
        String gatewayAccountId1 = randomAlphanumeric(15);
        String gatewayAccountId2 = randomAlphanumeric(15);

        metadataKeyDao.insertIfNotExist("test-key-1");
        metadataKeyDao.insertIfNotExist("test-key-2");
        metadataKeyDao.insertIfNotExist("test-key-3");

        gatewayAccountMetadataDao.upsert(gatewayAccountId1, "test-key-1");
        gatewayAccountMetadataDao.upsert(gatewayAccountId1, "test-key-2");
        gatewayAccountMetadataDao.upsert(gatewayAccountId2, "test-key-3");

        List<String> keysForGatewayAccount =
                gatewayAccountMetadataDao.findMetadataKeysForGatewayAccounts(List.of(gatewayAccountId1));

        MatcherAssert.assertThat(keysForGatewayAccount.size(), is(2));

        assertThat(keysForGatewayAccount, hasItem("test-key-1"));
        assertThat(keysForGatewayAccount, hasItem("test-key-2"));
    }

    @Test
    public void shouldNotInsertAnotherWhenARecordExistsAlreadyForGatewayAccountAndMetadataKey() {
        String gatewayAccountId = randomAlphanumeric(15);

        metadataKeyDao.insertIfNotExist("test-key-1");
        aGatewayAccountMetadataFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withMetadataKey("test-key-1")
                .insert(rule.getJdbi());

        gatewayAccountMetadataDao.upsert(gatewayAccountId, "test-key-1");

        List<Map<String, Object>> gatewayAccountMetadata = dbHelper.getGatewayAccountMetadata(gatewayAccountId, "test-key-1");
        assertThat(gatewayAccountMetadata.size(), is(1));
        assertThat(gatewayAccountMetadata.get(0).get("metadata_key_id"), is(notNullValue()));
        assertThat(gatewayAccountMetadata.get(0).get("gateway_account_id"), is(gatewayAccountId));
    }

    @Test
    public void shouldInsertWhenARecordDoesNotExistsForGatewayAccountAndMetadataKey() {
        String gatewayAccountId = randomAlphanumeric(15);
        metadataKeyDao.insertIfNotExist("test-key-2");

        gatewayAccountMetadataDao.upsert(gatewayAccountId, "test-key-2");

        List<Map<String, Object>> gatewayAccountMetadata = dbHelper.getGatewayAccountMetadata(gatewayAccountId, "test-key-2");
        assertThat(gatewayAccountMetadata.get(0).get("metadata_key_id"), is(notNullValue()));
        assertThat(gatewayAccountMetadata.get(0).get("gateway_account_id"), is(gatewayAccountId));
    }
}