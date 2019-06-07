package uk.gov.pay.ledger.event.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface ResourceTypeDao {

    @SqlQuery("SELECT id FROM resource_type WHERE upper(name) = :name")
    int getResourceTypeIdByName(@Bind("name") String name);
}
