package uk.gov.pay.ledger.util.fixture;

import org.jdbi.v3.core.Jdbi;

public class MetadataKeyFixture {

    public static void insertMedataKeyIfNotExists(Jdbi jdbi, String metadataKey) {
        jdbi.withHandle(h ->
                h.execute(
                        "INSERT INTO metadata_key(key) " +
                                "SELECT ? " +
                                "WHERE NOT EXISTS ( " +
                                "    SELECT 1 " +
                                "    FROM metadata_key " +
                                "    WHERE key = ? )"
                        ,
                        metadataKey,
                        metadataKey
                )
        );
    }
}
