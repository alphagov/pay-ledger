package uk.gov.pay.ledger.transactionmetadata.dao;

import com.google.inject.Inject;
import org.jdbi.v3.core.Jdbi;

public class TransactionMetadataDao {

    private static final String UPSERT_STRING = "INSERT INTO transaction_metadata(transaction_id, metadata_key_id, value) " +
            "VALUES(:transactionId, (select id from metadata_key where key = :key ), :value) " +
            "ON CONFLICT ON CONSTRAINT transaction_id_and_metadata_key_id_key " +
            "DO UPDATE SET value = EXCLUDED.value " +
            "WHERE transaction_metadata.transaction_id = EXCLUDED.transaction_id " +
            "AND transaction_metadata.metadata_key_id = EXCLUDED.metadata_key_id";

    private final Jdbi jdbi;

    @Inject
    public TransactionMetadataDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void upsert(Long transactionId, String key, String value) {
        jdbi.withHandle(handle ->
                handle.createUpdate(UPSERT_STRING)
                        .bind("transactionId", transactionId)
                        .bind("key", key)
                        .bind("value", value)
                        .execute()
        );
    }
}