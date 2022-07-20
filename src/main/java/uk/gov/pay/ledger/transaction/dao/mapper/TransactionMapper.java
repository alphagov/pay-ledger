package uk.gov.pay.ledger.transaction.dao.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.service.payments.commons.model.Source;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static uk.gov.pay.ledger.payout.entity.PayoutEntity.PayoutEntityBuilder.aPayoutEntity;
import static uk.gov.pay.ledger.util.dao.MapperUtils.getBooleanWithNullCheck;

public class TransactionMapper implements RowMapper<TransactionEntity> {

    @Override
    public TransactionEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
        var transactionBuilder = new TransactionEntity.Builder()
                .withId(rs.getLong("id"))
                .withServiceId(rs.getString("service_id"))
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
                .withLive(getBooleanWithNullCheck(rs,"live"))
                .withMoto(rs.getBoolean("moto"))
                .withGatewayTransactionId(rs.getString("gateway_transaction_id"))
                .withGatewayPayoutId(rs.getString("gateway_payout_id"))
                .withAgreementId(rs.getString("agreement_id"));
        Source.from(rs.getString("source")).ifPresent(transactionBuilder::withSource);
        if (rs.getString("gateway_payout_id") != null) {
            var payoutBuilder = aPayoutEntity()
                    .withGatewayPayoutId(rs.getString("gateway_payout_id"))
                    .withPaidOutDate(getZonedDateTime(rs, "paid_out_date").orElse(null));
            transactionBuilder.withPayoutEntity(payoutBuilder.build());
        }
        return transactionBuilder.build();
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
