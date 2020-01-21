package uk.gov.pay.ledger.metadata.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.Optional;

public interface MetadataDao {

    @SqlUpdate("INSERT INTO metadata(name) " +
            "SELECT :name " +
            "WHERE NOT EXISTS ( " +
            "    SELECT 1 " +
            "    FROM metadata " +
            "    WHERE name = :name )" )
    @GetGeneratedKeys
    Optional<Long> insertIfNotExist(@Bind("name") String metadataKey);
}
