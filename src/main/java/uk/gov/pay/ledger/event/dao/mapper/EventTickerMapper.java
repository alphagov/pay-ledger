package uk.gov.pay.ledger.event.dao.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventTicker;
import uk.gov.pay.ledger.event.model.ResourceType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class EventTickerMapper implements RowMapper<EventTicker> {

    @Override
    public EventTicker map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new EventTicker(resultSet.getLong("id"),
                ResourceType.valueOf(resultSet.getString("type").toUpperCase()),
                resultSet.getString("resource_external_id"),
                ZonedDateTime.ofInstant(resultSet.getTimestamp("event_date").toInstant(), ZoneOffset.UTC),
                resultSet.getString("event_type")
        );
    }
}
