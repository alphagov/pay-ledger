package uk.gov.pay.ledger.utils;

import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.event.model.ResourceType;

import java.util.Map;

public class DatabaseTestHelper {

    private Jdbi jdbi;

    private DatabaseTestHelper(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public static DatabaseTestHelper aDatabaseTestHelper(Jdbi jdbi) {
        return new DatabaseTestHelper(jdbi);
    }

    public Map<String, Object> getEventByExternalId(String externalId) {
        return jdbi.withHandle(handle ->
                handle
                        .createQuery("SELECT * FROM event WHERE resource_external_id = :external_id")
                        .bind("external_id", externalId)
                        .mapToMap()
                        .findFirst()
                        .get()
        );
    }
}
