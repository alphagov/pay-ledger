package uk.gov.pay.ledger.util;

import org.jdbi.v3.core.Jdbi;

import java.util.List;
import java.util.Map;

public class DatabaseTestHelper {

    private Jdbi jdbi;

    private DatabaseTestHelper(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public static DatabaseTestHelper aDatabaseTestHelper(Jdbi jdbi) {
        return new DatabaseTestHelper(jdbi);
    }

    public List<Map<String, Object>> getEventsByExternalId(String externalId) {
        return jdbi.withHandle(handle ->
                handle
                        .createQuery("SELECT * FROM event WHERE resource_external_id = :external_id")
                        .bind("external_id", externalId)
                        .mapToMap()
                        .list()
        );
    }

    public Map<String, Object> getEventByExternalId(String externalId) {
        return getEventsByExternalId(externalId).stream().findFirst().get();
    }

    public void truncateAllData() {
        jdbi.withHandle(h -> h.createScript(
                "TRUNCATE TABLE event CASCADE; " +
                        "TRUNCATE TABLE transaction CASCADE"
        ).execute());
    }

    public int getEventsCountByExternalId(String externalId) {
        return jdbi.withHandle(handle ->
                handle
                        .createQuery("SELECT COUNT(*) FROM event WHERE resource_external_id = :external_id")
                        .bind("external_id", externalId)
                        .mapTo(Integer.class)
                        .one()
        );
    }

    public List<Map<String, Object>> getAllTransactions() {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM transaction")
                        .mapToMap()
                        .list());
    }

    public List<Map<String, Object>> getMetadataKey(String key) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM metadata_key where key = :key")
                        .bind("key", key)
                        .mapToMap()
                        .list());
    }

    public List<Map<String, Object>> getTransactionMetadata(Long transactionId, String key) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM transaction_metadata where transaction_id = :transactionId" +
                        " and metadata_key_id = (select id from metadata_key where key = :key)")
                        .bind("transactionId", transactionId)
                        .bind("key", key)
                        .mapToMap()
                        .list());
    }
}
