package uk.gov.pay.ledger.transaction.dao.mapper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.transaction.model.Address;
import uk.gov.pay.ledger.transaction.model.CardDetails;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

public class TransactionMapper implements RowMapper<Transaction> {

    @Override
    public Transaction map(ResultSet rs, StatementContext ctx) throws SQLException {
        JsonObject transactionDetail = new JsonParser().parse(rs.getString("transaction_details")).getAsJsonObject();
        Address billingAddress = new Address(
                safeGetAsString(transactionDetail, "address_line1"),
                safeGetAsString(transactionDetail, "address_line2"),
                safeGetAsString(transactionDetail, "address_postcode"),
                safeGetAsString(transactionDetail, "address_city"),
                safeGetAsString(transactionDetail, "address_county"),
                safeGetAsString(transactionDetail, "address_country")
        );

        CardDetails cardDetails = new CardDetails(
                rs.getString("cardholder_name"),
                billingAddress,
                null);

        return new Transaction(
                rs.getLong("id"),
                rs.getString("gateway_account_id"),
                rs.getLong("amount"),
                rs.getString("reference"),
                rs.getString("description"),
                TransactionState.valueOf(rs.getString("state")),
                safeGetAsString(transactionDetail, "language"),
                rs.getString("external_id"),
                safeGetAsString(transactionDetail, "return_url"),
                rs.getString( "email"),
                safeGetAsString(transactionDetail, "payment_provider"),
                ZonedDateTime.ofInstant(rs.getTimestamp("created_date").toInstant(), ZoneOffset.UTC),
                cardDetails,
                safeGetJsonElement(transactionDetail, "delayed_capture")
                        .map(JsonElement::getAsBoolean)
                        .orElse(null),
                rs.getString("external_metadata"),
                rs.getInt("event_count")
        );
    }

    private String safeGetAsString(JsonObject object, String propertyName) {
        return safeGetJsonElement(object, propertyName)
                .map(JsonElement::getAsString)
                .orElse(null);
    }

    private Optional<JsonElement> safeGetJsonElement(JsonObject object, String propertyName) {
        return Optional.ofNullable(object.get(propertyName))
                .filter(p -> !p.isJsonNull());
    }
}
