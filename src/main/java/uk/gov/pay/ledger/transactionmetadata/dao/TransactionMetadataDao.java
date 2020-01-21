package uk.gov.pay.ledger.transactionmetadata.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.Optional;

public interface TransactionMetadataDao {
    @SqlUpdate("INSERT INTO transaction_metadata(transaction_id, metadata_id) " +
            "SELECT :transactionId, (select id from metadata where name = :metadataId) " +
            "WHERE NOT EXISTS ( " +
            "    SELECT 1 " +
            "    FROM transaction_metadata " +
            "    WHERE metadata_id in (select id from metadata where name = :metadataId ) and transaction_id = :transactionId)")
    @GetGeneratedKeys
    Optional<Long> insertIfNotExist(@Bind("transactionId") Long transactionId, @Bind("metadataId") String metadataId);
}
