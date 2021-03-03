package uk.gov.pay.ledger.metadatakey.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

public interface MetadataKeyDao {

    @SqlUpdate("INSERT INTO metadata_key(key) " +
            "SELECT :key " +
            "WHERE NOT EXISTS ( " +
            "    SELECT 1 " +
            "    FROM metadata_key " +
            "    WHERE key = :key )")
    @GetGeneratedKeys
    Optional<Long> insertIfNotExist(@Bind("key") String key);
}