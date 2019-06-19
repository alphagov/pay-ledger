package uk.gov.pay.ledger.transaction.dao.mapper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.transaction.model.Address;
import uk.gov.pay.ledger.transaction.model.CardDetails;
import uk.gov.pay.ledger.transaction.model.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

public class TransactionMapper implements RowMapper<Transaction> {

    @Override
    public Transaction map(ResultSet rs, StatementContext ctx) throws SQLException {
        JsonObject transactionDetail = new JsonParser().parse(rs.getString("transaction_details")).getAsJsonObject();
        Address billingAddress = new Address(
                getAsString(transactionDetail, "address_line1"),
                getAsString(transactionDetail, "address_line2"),
                getAsString(transactionDetail, "address_postcode"),
                getAsString(transactionDetail, "address_city"),
                getAsString(transactionDetail, "address_county"),
                getAsString(transactionDetail, "address_country")
        );

        CardDetails cardDetails = new CardDetails(
                rs.getString("cardholder_name"),
                billingAddress,
                null);

        Transaction transaction = new Transaction(
                rs.getLong("id"),
                rs.getString("gateway_account_id"),
                rs.getLong("amount"),
                rs.getString("reference"),
                rs.getString("description"),
                rs.getString("status"),
                getAsString(transactionDetail, "language"),
                rs.getString("external_id"),
                getAsString(transactionDetail, "return_url"),
                rs.getString( "email"),
                getAsString(transactionDetail, "payment_provider"),
                ZonedDateTime.ofInstant(rs.getTimestamp("created_date").toInstant(), ZoneOffset.UTC),
                cardDetails,

                Objects.nonNull(transactionDetail.get("delayed_capture")) ?
                        transactionDetail.get(  "delayed_capture").getAsBoolean() :
                        null,
                rs.getString("external_metadata")
        );

        return transaction;
    }

    private String getAsString(JsonObject object, String propertyName) {
        JsonElement property = object.get(propertyName);

        if (property != null && !property.isJsonNull()) {
            return property.getAsString();
        }

        return null;
    }
}
