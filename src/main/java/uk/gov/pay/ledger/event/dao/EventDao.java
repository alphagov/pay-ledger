package uk.gov.pay.ledger.event.dao;

import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import uk.gov.pay.ledger.event.dao.mapper.EventMapper;
import uk.gov.pay.ledger.event.model.Event;

import java.util.List;
import java.util.Optional;

@RegisterRowMapper(EventMapper.class)
public interface EventDao {
    @CreateSqlObject
    ResourceTypeDao getResourceTypeDao();

    @SqlQuery("SELECT e.id, e.sqs_message_id, rt.name AS resource_type_name, e.resource_external_id, e.parent_resource_external_id," +
            " e.event_date, e.event_type, e.event_data" +
            " FROM event e, resource_type rt WHERE e.id = :eventId AND e.resource_type_id = rt.id")
    Optional<Event> getById(@Bind("eventId") Long eventId);

    @SqlUpdate("INSERT INTO event(sqs_message_id, resource_type_id, resource_external_id, parent_resource_external_id, " +
                "event_date, event_type, event_data) " +
            "VALUES (:sqsMessageId, :resourceTypeId, :resourceExternalId, :parentResourceExternalId, " +
                ":eventDate, :eventType, CAST(:eventData as jsonb))")
    @GetGeneratedKeys
    Long insert(@BindBean Event event, @Bind("resourceTypeId") int resourceTypeId);

    @SqlUpdate("INSERT INTO event(sqs_message_id, resource_type_id, resource_external_id, parent_resource_external_id, " +
            "event_date, event_type, event_data) " +
            "SELECT :sqsMessageId, :resourceTypeId, :resourceExternalId, :parentResourceExternalId, " +
            "       :eventDate, :eventType, CAST(:eventData as jsonb) " +
            "WHERE NOT EXISTS ( " +
            "    SELECT 1 " +
            "    FROM event " +
            "    WHERE resource_type_id = :resourceTypeId AND " +
            "          resource_external_id = :resourceExternalId AND  " +
            "          event_type = :eventType) ")
    @GetGeneratedKeys
    Optional<Long> insertIfDoesNotExist(@BindBean Event event, @Bind("resourceTypeId") int resourceTypeId);

    @Transaction
    default Long insertEventWithResourceTypeId(Event event) {
        int resourceTypeId = getResourceTypeDao().getResourceTypeIdByName(event.getResourceType().name());
        return insert(event, resourceTypeId);
    }

    @Transaction
    default Optional<Long> insertEventIfDoesNotExistWithResourceTypeId(Event event) {
        int resourceTypeId = getResourceTypeDao().getResourceTypeIdByName(event.getResourceType().name());
        return insertIfDoesNotExist(event, resourceTypeId);
    }

    @SqlQuery("SELECT  e.id, e.sqs_message_id, rt.name AS resource_type_name, e.resource_external_id, " +
            "e.parent_resource_external_id, e.event_date," +
            "e.event_type, e.event_data FROM event e, resource_type rt WHERE e.resource_external_id = :resourceExternalId" +
            " AND e.resource_type_id = rt.id ORDER BY e.event_date DESC")
    List<Event> getEventsByResourceExternalId(@Bind("resourceExternalId") String resourceExternalId);
}
