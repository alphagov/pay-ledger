package uk.gov.pay.ledger.agreement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.agreement.dao.AgreementDao;
import uk.gov.pay.ledger.agreement.dao.PaymentInstrumentDao;
import uk.gov.pay.ledger.agreement.entity.PaymentInstrumentEntity;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.util.fixture.AgreementFixture;
import uk.gov.pay.ledger.util.fixture.PaymentInstrumentFixture;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class PaymentInstrumentDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private final AgreementDao agreementDao = new AgreementDao(rule.getJdbi());
    private final PaymentInstrumentDao paymentInstrumentDao = new PaymentInstrumentDao(rule.getJdbi());

    @Test
    void shouldInsertPaymentInstrument() {
        var paymentInstrumentExternalId = "pi-external-id";
        AgreementFixture agreementFixture = AgreementFixture.anAgreementFixture();
        var paymentInstrumentFixture = PaymentInstrumentFixture.aPaymentInstrumentFixture(
                paymentInstrumentExternalId,
                agreementFixture.getExternalId(),
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        agreementDao.upsert(agreementFixture.toEntity());
        paymentInstrumentDao.upsert(paymentInstrumentFixture.toEntity());

        PaymentInstrumentEntity fetchedEntity = agreementDao.findByExternalId(agreementFixture.getExternalId()).get().getPaymentInstrument();

        assertThat(fetchedEntity.getExternalId(), is(paymentInstrumentExternalId));
    }
}