package uk.gov.pay.ledger.transactionevent.dao.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.transactionevent.model.TransactionEventEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class TransactionEventEntityMapper implements RowMapper<TransactionEventEntity> {

    @Override
    public TransactionEventEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new TransactionEventEntity.Builder()
                .withExternalId(rs.getString("external_id"))
                .withAmount(rs.getLong("amount"))
                .withEventType(rs.getString("event_type"))
                .withEventData(rs.getString("event_data"))
                .withEventDate(getZonedDateTime(rs, "event_date"))
                .withTransactionType(rs.getString("type"))
                .build();
    }

    private ZonedDateTime getZonedDateTime(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnLabel);
        return ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC);
    }
}
