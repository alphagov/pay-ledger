package uk.gov.pay.ledger.transactionmetadata.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

public interface TransactionMetadataDao {

    @SqlUpdate("INSERT INTO transaction_metadata(transaction_id, metadata_key_id) " +
            "SELECT :transactionId, (select id from metadata_key where key = :key) " +
            "WHERE NOT EXISTS ( " +
            "    SELECT 1 " +
            "    FROM transaction_metadata " +
            "    WHERE metadata_key_id in (select id from metadata_key where key = :key ) " +
            "      and transaction_id = :transactionId)")
    @GetGeneratedKeys
    Optional<Long> insertIfNotExist(@Bind("transactionId") Long transactionId,
                                    @Bind("key") String key);
}