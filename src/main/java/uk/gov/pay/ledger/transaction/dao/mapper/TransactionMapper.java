package uk.gov.pay.ledger.transaction.dao.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class TransactionMapper implements RowMapper<TransactionEntity> {

    @Override
    public TransactionEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new TransactionEntity(
                rs.getLong("id"),
                rs.getString("gateway_account_id"),
                rs.getString("external_id"),
                rs.getLong("amount"),
                rs.getString("reference"),
                rs.getString("description"),
                rs.getString("state"),
                rs.getString( "email"),
                rs.getString("cardholder_name"),
                rs.getString("external_metadata"),
                getZonedDateTime(rs, "created_date"),
                rs.getString("transaction_details"),
                rs.getInt("event_count"),
                rs.getString("card_brand"),
                rs.getString("last_digits_card_number"),
                rs.getString("first_digits_card_number"),
                rs.getLong("net_amount"),
                rs.getLong("total_amount"),
                getZonedDateTime(rs, "settlement_submitted_time"),
                getZonedDateTime(rs, "settled_time"),
                rs.getString("refund_status"),
                rs.getLong("refund_amount_submitted"),
                rs.getLong("refund_amount_available")
        );
    }

    private ZonedDateTime getZonedDateTime(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnLabel);

        return timestamp != null ? ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC) : null;
    }
}
