package uk.gov.pay.ledger.transaction.dao.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.common.TransactionState;
import uk.gov.pay.ledger.transaction.TransactionView;
import uk.gov.pay.ledger.transaction.model.Address;
import uk.gov.pay.ledger.transaction.model.CardDetails;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class TransactionViewMapper implements RowMapper<TransactionView> {

    @Override
    public TransactionView map(ResultSet rs, StatementContext ctx) throws SQLException {
        Address billingAddress = new Address(
                rs.getString("address_line1"),
                rs.getString("address_line2"),
                rs.getString("address_postcode"),
                rs.getString("address_city"),
                rs.getString("address_county"),
                rs.getString("address_country"));

        CardDetails cardDetails = new CardDetails(
                rs.getString("cardholder_name"),
                billingAddress,
                null);

        TransactionView transaction = new TransactionView(
                rs.getLong("id"),
                rs.getString("gateway_account_id"),
                rs.getLong("amount"),
                TransactionState.valueOf(rs.getString("status").toUpperCase()),
                rs.getString("description"),
                rs.getString("reference"),
                rs.getString("language"),
                rs.getString("external_id"),
                rs.getString("return_url"),
                rs.getString("email"),
                rs.getString("payment_provider"),
                ZonedDateTime.ofInstant(rs.getTimestamp("created_date").toInstant(), ZoneOffset.UTC),
                cardDetails,
                rs.getBoolean("delayed_capture")
        );

        return transaction;
    }
}
