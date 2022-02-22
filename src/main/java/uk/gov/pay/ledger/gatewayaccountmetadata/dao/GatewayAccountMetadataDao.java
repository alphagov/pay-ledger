package uk.gov.pay.ledger.gatewayaccountmetadata.dao;

import com.google.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;

import java.util.List;

public class GatewayAccountMetadataDao {

    private static final String FIND_METADATA_KEYS = "SELECT distinct mk.key FROM gateway_account_metadata gam, metadata_key mk " +
            " WHERE gam.metadata_key_id = mk.id" +
            " AND gateway_account_id in (<gatewayAccountIDs>) ";

    private static final String UPSERT_STRING = "INSERT INTO gateway_account_metadata(gateway_account_id, metadata_key_id) " +
            "VALUES(:gatewayAccountId, (select id from metadata_key where key = :key )) " +
            "ON CONFLICT ON CONSTRAINT gateway_account_id_and_metadata_key_id_key " +
            "DO NOTHING ";

    private final Jdbi jdbi;

    @Inject
    public GatewayAccountMetadataDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void upsert(String gatewayAccountId, String key) {
        jdbi.withHandle(handle ->
                handle.createUpdate(UPSERT_STRING)
                        .bind("gatewayAccountId", gatewayAccountId)
                        .bind("key", key)
                        .execute()
        );
    }

    public List<String> findMetadataKeysForGatewayAccounts(List<String> gatewayAccountIDs) {
        return jdbi.withHandle(handle -> {

            Query query = handle.createQuery(FIND_METADATA_KEYS);
            query.bindList("gatewayAccountIDs", gatewayAccountIDs);

            return query
                    .mapTo(String.class)
                    .list();
        });
    }
}