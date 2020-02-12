package uk.gov.pay.ledger.transaction.dao.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

public class TransactionWithParentMapper implements RowMapper<TransactionEntity> {

    @Override
    public TransactionEntity map(ResultSet rs, StatementContext ctx) throws SQLException {

        TransactionEntity parentTransactionEntity = new TransactionEntity.Builder()
                .withId(getLongWithNullCheck(rs, "parent_id"))
                .withGatewayAccountId(rs.getString("parent_gateway_account_id"))
                .withExternalId(rs.getString("parent_external_id"))
                .withParentExternalId(rs.getString("parent_parent_external_id"))
                .withAmount(getLongWithNullCheck(rs, "parent_amount"))
                .withReference(rs.getString("parent_reference"))
                .withDescription(rs.getString("parent_description"))
                .withState(getState(rs, "parent_state"))
                .withEmail(rs.getString("parent_email"))
                .withCardholderName(rs.getString("parent_cardholder_name"))
                .withCreatedDate(getZonedDateTime(rs, "parent_created_date").orElse(null))
                .withTransactionDetails(rs.getString("parent_transaction_details"))
                .withEventCount(rs.getInt("parent_event_count"))
                .withCardBrand(rs.getString("parent_card_brand"))
                .withLastDigitsCardNumber(rs.getString("parent_last_digits_card_number"))
                .withFirstDigitsCardNumber(rs.getString("parent_first_digits_card_number"))
                .withNetAmount(getLongWithNullCheck(rs, "parent_net_amount"))
                .withTotalAmount(getLongWithNullCheck(rs, "parent_total_amount"))
                .withRefundStatus(rs.getString("parent_refund_status"))
                .withRefundAmountRefunded(getLongWithNullCheck(rs, "parent_refund_amount_refunded"))
                .withRefundAmountAvailable(getLongWithNullCheck(rs, "parent_refund_amount_available"))
                .withFee(getLongWithNullCheck(rs, "parent_fee"))
                .withTransactionType(rs.getString("parent_type"))
                .withLive(rs.getBoolean("live"))
                .withMoto(rs.getBoolean("parent_moto"))
                .build();

        return new TransactionEntity.Builder()
                .withId(rs.getLong("id"))
                .withGatewayAccountId(rs.getString("gateway_account_id"))
                .withExternalId(rs.getString("external_id"))
                .withParentExternalId(rs.getString("parent_external_id"))
                .withAmount(getLongWithNullCheck(rs, "amount"))
                .withReference(rs.getString("reference"))
                .withDescription(rs.getString("description"))
                .withState(getState(rs, "state"))
                .withEmail(rs.getString("email"))
                .withCardholderName(rs.getString("cardholder_name"))
                .withCreatedDate(getZonedDateTime(rs, "created_date").orElse(null))
                .withTransactionDetails(rs.getString("transaction_details"))
                .withEventCount(getIntegerWithNullCheck(rs, "event_count"))
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
                .withParentTransactionEntity(parentTransactionEntity)
                .build();
    }

    private TransactionState getState(ResultSet rs, String columnName) throws SQLException {
        String state = rs.getString(columnName);
        return Optional.ofNullable(state)
                .map(TransactionState::from)
                .orElse(null);
    }

    private Long getLongWithNullCheck(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private Integer getIntegerWithNullCheck(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private Optional<ZonedDateTime> getZonedDateTime(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnLabel);

        return Optional.ofNullable(timestamp)
                .map(t -> ZonedDateTime.ofInstant(t.toInstant(), ZoneOffset.UTC));
    }
}
