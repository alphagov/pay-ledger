package uk.gov.pay.ledger.event.dao.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class EventMapper implements RowMapper<Event> {

    @Override
    public Event map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new Event(resultSet.getLong("id"),
                resultSet.getString("sqs_message_id"),
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
