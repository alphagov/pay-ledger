package uk.gov.pay.ledger.agreement.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.agreement.entity.AgreementEntity;
import uk.gov.pay.ledger.agreement.entity.PaymentInstrumentEntity;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static uk.gov.pay.ledger.util.dao.MapperUtils.getBooleanWithNullCheck;

public class AgreementMapper implements RowMapper<AgreementEntity> {
    @Override
    public AgreementEntity map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        PaymentInstrumentEntity paymentInstrument = null;
        var paymentInstrumentExternalId = resultSet.getString("p_external_id");

        if (paymentInstrumentExternalId != null) {
            paymentInstrument = new PaymentInstrumentEntity(
                    resultSet.getString("p_external_id"),
                    resultSet.getString("p_agreement_external_id"),
                    resultSet.getString("p_email"),
                    resultSet.getString("p_cardholder_name"),
                    resultSet.getString("p_address_line1"),
                    resultSet.getString("p_address_line2"),
                    resultSet.getString("p_address_postcode"),
                    resultSet.getString("p_address_city"),
                    resultSet.getString("p_address_county"),
                    resultSet.getString("p_address_country"),
                    resultSet.getString("p_last_digits_card_number"),
                    resultSet.getString("p_expiry_date"),
                    resultSet.getString("p_card_brand"),
                    ZonedDateTime.ofInstant(resultSet.getTimestamp("p_created_date").toInstant(), ZoneOffset.UTC),
                    resultSet.getInt("p_event_count")
            );
        }
        return new AgreementEntity(
                resultSet.getString("external_id"),
                resultSet.getString("gateway_account_id"),
                resultSet.getString("service_id"),
                resultSet.getString("reference"),
                resultSet.getString("description"),
                resultSet.getString("status"),
                getBooleanWithNullCheck(resultSet,"live"),
                ZonedDateTime.ofInstant(resultSet.getTimestamp("created_date").toInstant(), ZoneOffset.UTC),
                resultSet.getInt("event_count"),
                paymentInstrument
        );
    }
}