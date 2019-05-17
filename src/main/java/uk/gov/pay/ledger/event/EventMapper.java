package uk.gov.pay.ledger.event;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EventMapper implements RowMapper<Event> {
    @Override
    public Event map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new Event(resultSet.getString("id"));
    }
}
