package uk.gov.pay.ledger.transaction.dao.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.commons.model.Source;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

public class TransactionMapper implements RowMapper<TransactionEntity> {

    @Override
    public TransactionEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
        var builder = new TransactionEntity.Builder()
                .withId(rs.getLong("id"))
                .withGatewayAccountId(rs.getString("gateway_account_id"))
                .withExternalId(rs.getString("external_id"))
                .withParentExternalId(rs.getString("parent_external_id"))
                .withAmount(getLongWithNullCheck(rs, "amount"))
                .withReference(rs.getString("reference"))
                .withDescription(rs.getString("description"))
                .withState(TransactionState.from(rs.getString("state")))
                .withEmail(rs.getString("email"))
                .withCardholderName(rs.getString("cardholder_name"))
                .withCreatedDate(getZonedDateTime(rs, "created_date").orElse(null))
                .withTransactionDetails(rs.getString("transaction_details"))
                .withEventCount(rs.getInt("event_count"))
                .withCardBrand(rs.getString("card_brand"))
                .withLastDigitsCardNumber(rs.getString("last_digits_card_number"))
                .withFirstDigitsCardNumber(rs.getString("first_digits_card_number"))
                .withNetAmount(getLongWithNullCheck(rs, "net_amount"))
                .withTotalAmount(getLongWithNullCheck(rs, "total_amount"))
                .withRefundStatus(rs.getString("refund_status"))
                .withRefundAmountRefunded(getLongWithNullCheck(rs, "refund_amount_refunded"))
                .withRefundAmountAvailable(getLongWithNullCheck(rs, "refund_amount_available"))
                .withFee(getLongWithNullCheck(rs, "fee"))
                .withTransactionType(rs.getString("type"))
                .withLive(rs.getBoolean("live"))
                .withMoto(rs.getBoolean("moto"))
                .withGatewayTransactionId(rs.getString("gateway_transaction_id"))
                .withGatewayPayoutId(rs.getString("gateway_payout_id"));
        Source.from(rs.getString("source")).ifPresent(builder::withSource);
        return builder.build();
    }

    private Long getLongWithNullCheck(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private Optional<ZonedDateTime> getZonedDateTime(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnLabel);

        return Optional.ofNullable(timestamp)
                .map(t -> ZonedDateTime.ofInstant(t.toInstant(), ZoneOffset.UTC));
    }
}
