package uk.gov.pay.ledger.event.dao.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.event.dao.entity.EventEntity;
import uk.gov.pay.ledger.event.model.ResourceType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static uk.gov.pay.ledger.util.dao.MapperUtils.getBooleanWithNullCheck;

public class EventMapper implements RowMapper<EventEntity> {

    @Override
    public EventEntity map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new EventEntity(resultSet.getLong("id"),
                resultSet.getString("sqs_message_id"),
                resultSet.getString("service_id"),
                getBooleanWithNullCheck(resultSet, "live"),
                ResourceType.valueOf(resultSet.getString("resource_type_name").toUpperCase()),
                resultSet.getString("resource_external_id"),
                resultSet.getString("parent_resource_external_id"),
                ZonedDateTime.ofInstant(resultSet.getTimestamp("event_date").toInstant(), ZoneOffset.UTC),
                resultSet.getString("event_type"),
                resultSet.getString("event_data"),
                false
        );
    }
}
