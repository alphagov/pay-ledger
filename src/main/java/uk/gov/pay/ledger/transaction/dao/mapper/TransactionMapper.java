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
                .withId(rs.getLong("id"))
                .withGatewayAccountId(rs.getString("gateway_account_id"))
                .withExternalId(rs.getString("external_id"))
                .withParentExternalId(rs.getString("parent_external_id"))
                .withAmount(rs.getLong("amount"))
                .withReference(rs.getString("reference"))
                .withDescription(rs.getString("description"))
                .withState(rs.getString("state"))
                .withEmail(rs.getString("email"))
                .withCardholderName(rs.getString("cardholder_name"))
                .withExternalMetadata(rs.getString("external_metadata"))
                .withCreatedDate(getZonedDateTime(rs, "created_date").orElse(null))
                .withTransactionDetails(rs.getString("transaction_details"))
                .withEventCount(rs.getInt("event_count"))
                .withCardBrand(rs.getString("card_brand"))
                .withLastDigitsCardNumber(rs.getString("last_digits_card_number"))
                .withFirstDigitsCardNumber(rs.getString("first_digits_card_number"))
                .withNetAmount(rs.getLong("net_amount"))
                .withTotalAmount(rs.getLong("total_amount"))
                .withSettlementSubmittedTime(getZonedDateTime(rs, "settlement_submitted_time").orElse(null))
                .withSettledTime(getZonedDateTime(rs, "settled_time").orElse(null))
                .withRefundStatus(rs.getString("refund_status"))
                .withRefundAmountSubmitted(rs.getLong("refund_amount_submitted"))
                .withRefundAmountAvailable(rs.getLong("refund_amount_available"))
                .withFee(rs.getLong("fee"))
                .withTransactionType(rs.getString("type"))
                .build();
    }

    private Optional<ZonedDateTime> getZonedDateTime(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnLabel);

        return Optional.ofNullable(timestamp)
                .map(t -> ZonedDateTime.ofInstant(t.toInstant(), ZoneOffset.UTC));
    }
}
