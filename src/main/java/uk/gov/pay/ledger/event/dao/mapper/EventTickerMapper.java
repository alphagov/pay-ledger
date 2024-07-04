package uk.gov.pay.ledger.event.dao.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.event.model.EventTicker;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.service.payments.commons.model.AuthorisationMode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Objects;

import static uk.gov.service.payments.commons.model.AuthorisationMode.AGREEMENT;


public class EventTickerMapper implements RowMapper<EventTicker> {

    @Override
    public EventTicker map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        Boolean isRecurring = Objects.equals(resultSet.getString("authorisation_mode"), AGREEMENT.getName());
        return new EventTicker(resultSet.getLong("id"),
                ResourceType.valueOf(resultSet.getString("type").toUpperCase(Locale.UK)),
                resultSet.getString("resource_external_id"),
                ZonedDateTime.ofInstant(resultSet.getTimestamp("event_date").toInstant(), ZoneOffset.UTC),
                resultSet.getString("event_type"),
                resultSet.getString("card_brand"),
                resultSet.getString("type"),
                resultSet.getString("payment_provider"),
                resultSet.getString("gateway_account_id"),
                resultSet.getLong("amount"),
                resultSet.getString("service_id"),
                resultSet.getString("wallet_type"),
                resultSet.getString("source"),
                resultSet.getBoolean("moto"),
                isRecurring
        );
    }
}
