package uk.gov.pay.ledger.event.dao;

import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import uk.gov.pay.ledger.event.dao.mapper.EventMapper;
import uk.gov.pay.ledger.event.dao.mapper.EventTickerMapper;
import uk.gov.pay.ledger.event.entity.EventEntity;
import uk.gov.pay.ledger.event.model.EventTicker;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static uk.gov.pay.ledger.transaction.service.TransactionService.REDACTED_REFERENCE_NUMBER;

@RegisterRowMapper(EventMapper.class)
@RegisterRowMapper(EventTickerMapper.class)
public interface EventDao {
    @CreateSqlObject
    ResourceTypeDao getResourceTypeDao();

    @SqlQuery("SELECT e.id, e.sqs_message_id, e.service_id, e.live, rt.name AS resource_type_name, e.resource_external_id, e.parent_resource_external_id," +
            " e.event_date, e.event_type, e.event_data" +
            " FROM event e, resource_type rt WHERE e.id = :eventId AND e.resource_type_id = rt.id")
    Optional<EventEntity> getById(@Bind("eventId") Long eventId);

    @SqlUpdate("INSERT INTO event(sqs_message_id, service_id, live, resource_type_id, resource_external_id, parent_resource_external_id, " +
                "event_date, event_type, event_data) " +
            "VALUES (:sqsMessageId, :serviceId, :live, :resourceTypeId, :resourceExternalId, :parentResourceExternalId, " +
                ":eventDate, :eventType, CAST(:eventData as jsonb))")
    @GetGeneratedKeys
    Long insert(@BindBean EventEntity event, @Bind("resourceTypeId") int resourceTypeId);

    @SqlUpdate("INSERT INTO event(sqs_message_id, service_id, live, resource_type_id, resource_external_id, parent_resource_external_id, " +
            "event_date, event_type, event_data) " +
            "SELECT :sqsMessageId, :serviceId, :live, :resourceTypeId, :resourceExternalId, :parentResourceExternalId, " +
            "       :eventDate, :eventType, CAST(:eventData as jsonb) " +
            "WHERE NOT EXISTS ( " +
            "    SELECT 1 " +
            "    FROM event " +
            "    WHERE resource_type_id = :resourceTypeId AND " +
            "          resource_external_id = :resourceExternalId AND  " +
            "          event_date = :eventDate AND   " +
            "          event_type = :eventType) ")
    @GetGeneratedKeys
    Optional<Long> insertIfDoesNotExist(@BindBean EventEntity event, @Bind("resourceTypeId") int resourceTypeId);

    @Transaction
    default Long insertEventWithResourceTypeId(EventEntity event) {
        int resourceTypeId = getResourceTypeDao().getResourceTypeIdByName(event.getResourceType().name());
        return insert(event, resourceTypeId);
    }

    @Transaction
    default Optional<Long> insertEventIfDoesNotExistWithResourceTypeId(EventEntity event) {
        int resourceTypeId = getResourceTypeDao().getResourceTypeIdByName(event.getResourceType().name());
        return insertIfDoesNotExist(event, resourceTypeId);
    }

    @SqlQuery("SELECT  e.id, e.sqs_message_id, e.service_id, e.live, rt.name AS resource_type_name, e.resource_external_id, " +
            "e.parent_resource_external_id, e.event_date," +
            "e.event_type, e.event_data FROM event e, resource_type rt WHERE e.resource_external_id = :resourceExternalId" +
            " AND e.resource_type_id = rt.id ORDER BY e.event_date DESC")
    List<EventEntity> getEventsByResourceExternalId(@Bind("resourceExternalId") String resourceExternalId);


    @SqlQuery("SELECT  e.id, e.sqs_message_id, e.service_id, e.live, rt.name AS resource_type_name, e.resource_external_id, " +
            "          e.parent_resource_external_id, e.event_date," +
            "          e.event_type, e.event_data FROM event e, resource_type rt" +
            " WHERE e.resource_external_id in (<externalIds>)" +
            " AND e.resource_type_id = rt.id" +
            " ORDER BY e.event_date ASC")
    List<EventEntity> findEventsForExternalIds(@BindList("externalIds") Set<String> externalIds);

    @SqlQuery("SELECT e.id, e.event_type, e.resource_external_id, e.event_date, t.card_brand, t.amount, " +
            "t.transaction_details->'payment_provider' as payment_provider, t.gateway_account_id, t.type " +
            "FROM event e LEFT JOIN transaction t ON e.resource_external_id = t.external_id " +
            "WHERE (e.event_date between :fromDate AND :toDate) AND t.live ORDER BY e.event_date DESC")
    List<EventTicker> findEventsTickerFromDate(@Bind("fromDate") ZonedDateTime fromDate, @Bind("toDate") ZonedDateTime toDate);

    @SqlUpdate("UPDATE event SET event_data = jsonb_set(event_data, '{reference}', '\"" + REDACTED_REFERENCE_NUMBER + "\"', false) " +
            "WHERE resource_external_id = :resourceExternalId")
    void redactReference(@Bind("resourceExternalId") String resourceExternalId);
}
