package uk.gov.pay.ledger.util.fixture;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.agreement.entity.AgreementEntity;
import uk.gov.pay.ledger.agreement.entity.PaymentInstrumentEntity;
import uk.gov.pay.ledger.transaction.model.CardDetails;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class PaymentInstrumentFixture implements DbFixture<PaymentInstrumentFixture, PaymentInstrumentEntity> {

    private Long id = RandomUtils.nextLong(1, 99999);
    private String externalId = RandomStringUtils.randomAlphanumeric(26);
    private String agreementExternalId = RandomStringUtils.randomAlphanumeric(26);
    private String email = "jdoe@email.com";
    private String cardholderName = "J Doe";
    private String addressLine1 = "Address line 1";
    private String addressLine2 = "Address line 2";
    private String addressPostcode = "EC3R8BT";
    private String addressCity = "London";
    private String addressCounty;
    private String addressCountry = "UK";
    private String lastDigitsCardNumber = "4242";
    private String expiryDate = "10/21";
    private String cardBrand = "visa";

    private Integer eventCount = 1;
    private ZonedDateTime createdDate = ZonedDateTime.now(ZoneOffset.UTC);

    private PaymentInstrumentFixture() {
    }

    public static PaymentInstrumentFixture aPaymentInstrumentFixture(String externalId, String agreementExternalId) {
        var fixture = new PaymentInstrumentFixture();
        fixture.setExternalId(externalId);
        fixture.setAgreementExternalId(agreementExternalId);
        return fixture;
    }

    public static PaymentInstrumentFixture aPaymentInstrumentFixture() {
        return new PaymentInstrumentFixture();
    }

    @Override
    public PaymentInstrumentFixture insert(Jdbi jdbi) {
        var sql = "INSERT INTO payment_instrument" +
                "(id, external_id, agreement_external_id, email, cardholder_name, address_line1, address_line2, address_postcode, address_city, address_county, address_country, last_digits_card_number, expiry_date, card_brand, event_count, created_date) " +
                "VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbi.withHandle(h ->
                h.execute(
                        sql,
                        id,
                        externalId,
                        agreementExternalId,
                        email,
                        cardholderName,
                        addressLine1,
                        addressLine2,
                        addressPostcode,
                        addressCity,
                        addressCounty,
                        addressCountry,
                        lastDigitsCardNumber,
                        expiryDate,
                        cardBrand,
                        eventCount,
                        createdDate
                )
        );
        return this;
    }

    @Override
    public PaymentInstrumentEntity toEntity() {
        return new PaymentInstrumentEntity(
                externalId,
                agreementExternalId,
                email,
                cardholderName,
                addressLine1,
                addressLine2,
                addressPostcode,
                addressCity,
                addressCounty,
                addressCountry,
                lastDigitsCardNumber,
                expiryDate,
                cardBrand,
                createdDate,
                eventCount
        );
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setAgreementExternalId(String agreementExternalId) {
        this.agreementExternalId = agreementExternalId;
    }
}