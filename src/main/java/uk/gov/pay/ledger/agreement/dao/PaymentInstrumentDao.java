package uk.gov.pay.ledger.agreement.dao;

import com.google.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.agreement.entity.AgreementEntity;
import uk.gov.pay.ledger.agreement.entity.PaymentInstrumentEntity;

public class PaymentInstrumentDao {
    private Jdbi jdbi;

    private final String UPSERT_AGREEMENT = "INSERT INTO payment_instrument " +
            "(" +
            "external_id," +
            "agreement_external_id, " +
            "email," +
            "cardholder_name," +
            "address_line1," +
            "address_line2," +
            "address_postcode," +
            "address_city," +
            "address_county," +
            "address_country," +
            "last_digits_card_number," +
            "expiry_date," +
            "card_brand," +
            "event_count," +
            "created_date" +
            ") " +
            "VALUES (" +
            ":externalId, " +
            ":agreementExternalId, " +
            ":email, " +
            ":cardholderName, " +
            ":addressLine1, " +
            ":addressLine2, " +
            ":addressPostcode, " +
            ":addressCity, " +
            ":addressCounty, " +
            ":addressCountry, " +
            ":lastDigitsCardNumber, " +
            ":expiryDate, " +
            ":cardBrand, " +
            ":eventCount, " +
            ":createdDate" +
            ") " +
            "ON CONFLICT (external_id) DO UPDATE SET " +
            "external_id = EXCLUDED.external_id," +
            "agreement_external_id = EXCLUDED.agreement_external_id," +
            "email = EXCLUDED.email," +
            "cardholder_name = EXCLUDED.cardholder_name," +
            "address_line1 = EXCLUDED.address_line1," +
            "address_line2 = EXCLUDED.address_line2," +
            "address_postcode = EXCLUDED.address_postcode," +
            "address_city = EXCLUDED.address_city," +
            "address_county = EXCLUDED.address_county," +
            "address_country = EXCLUDED.address_country," +
            "last_digits_card_number = EXCLUDED.last_digits_card_number," +
            "expiry_date = EXCLUDED.expiry_date," +
            "card_brand = EXCLUDED.card_brand," +
            "event_count = EXCLUDED.event_count," +
            "created_date = EXCLUDED.created_date " +
            "WHERE EXCLUDED.event_count >= payment_instrument.event_count";

    @Inject
    public PaymentInstrumentDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void upsert(PaymentInstrumentEntity paymentInstrument) {
        jdbi.withHandle(handle ->
                handle.createUpdate(UPSERT_AGREEMENT)
                        .bindBean(paymentInstrument)
                        .execute());
    }
}