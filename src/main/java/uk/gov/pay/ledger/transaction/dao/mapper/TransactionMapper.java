package uk.gov.pay.ledger.transaction.dao.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

public class TransactionMapper implements RowMapper<TransactionEntity> {

    @Override
    public TransactionEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new TransactionEntity.Builder()
                .id(rs.getLong("id"))
                .gatewayAccountId(rs.getString("gateway_account_id"))
                .externalId(rs.getString("external_id"))
                .amount(rs.getLong("amount"))
                .reference(rs.getString("reference"))
                .description(rs.getString("description"))
                .state(rs.getString("state"))
                .email(rs.getString("email"))
                .cardholderName(rs.getString("cardholder_name"))
                .externalMetadata(rs.getString("external_metadata"))
                .createdDate(getZonedDateTime(rs, "created_date"))
                .transactionDetails(rs.getString("transaction_details"))
                .eventCount(rs.getInt("event_count"))
                .cardBrand(rs.getString("card_brand"))
                .lastDigitsCardNumber(rs.getString("last_digits_card_number"))
                .firstDigitsCardNumber(rs.getString("first_digits_card_number"))
                .netAmount(rs.getLong("net_amount"))
                .totalAmount(rs.getLong("total_amount"))
                .settlementSubmittedTime(getZonedDateTime(rs, "settlement_submitted_time"))
                .settledTime(getZonedDateTime(rs, "settled_time"))
                .refundStatus(rs.getString("refund_status"))
                .refundAmountSubmitted(rs.getLong("refund_amount_submitted"))
                .refundAmountAvailable(rs.getLong("refund_amount_available"))
                .build();
    }

    private ZonedDateTime getZonedDateTime(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnLabel);

        return Optional.ofNullable(timestamp)
                .map(t -> ZonedDateTime.ofInstant(t.toInstant(), ZoneOffset.UTC))
                .orElse(null);
    }
}
