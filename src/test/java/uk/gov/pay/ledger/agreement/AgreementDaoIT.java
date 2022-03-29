package uk.gov.pay.ledger.agreement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.agreement.dao.AgreementDao;
import uk.gov.pay.ledger.agreement.dao.PaymentInstrumentDao;
import uk.gov.pay.ledger.agreement.entity.AgreementEntity;
import uk.gov.pay.ledger.agreement.resource.AgreementSearchParams;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.util.fixture.AgreementFixture;
import uk.gov.pay.ledger.util.fixture.PaymentInstrumentFixture;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class AgreementDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private AgreementDao agreementDao = new AgreementDao(rule.getJdbi());
    private PaymentInstrumentDao paymentInstrumentDao = new PaymentInstrumentDao(rule.getJdbi());

    @Test
    void shouldInsertAgreement() {
        AgreementFixture fixture = AgreementFixture.anAgreementFixture();
        var paymentInstrumentFixture = PaymentInstrumentFixture.aPaymentInstrumentFixture("pi-external-id", fixture.getExternalId());
        agreementDao.upsert(fixture.toEntity());
        paymentInstrumentDao.upsert(paymentInstrumentFixture.toEntity());

        AgreementEntity fetchedEntity = agreementDao.findByExternalId(fixture.getExternalId()).get();

        assertThat(fetchedEntity.getExternalId(), is(fixture.getExternalId()));
    }

    @Test
    void shouldSearchAgreement() {
        var fixture = AgreementFixture.anAgreementFixture("external-id", "service-id");
        var secondFixture = AgreementFixture.anAgreementFixture("second-external-id", "second-service-id");
        agreementDao.upsert(fixture.toEntity());
        agreementDao.upsert(secondFixture.toEntity());

        var searchParams = new AgreementSearchParams();
        searchParams.setServiceIds(List.of("service-id"));

        var missingParams = new AgreementSearchParams();
        missingParams.setServiceIds(List.of("missing-service-id"));

        var agreements = agreementDao.searchAgreements(searchParams);
        var count = agreementDao.getTotalForSearch(searchParams);
        assertThat(agreements.size(), is(1));
        assertThat(agreements.get(0).getExternalId(), is("external-id"));
        assertThat(count, is(1L));

        var missing = agreementDao.searchAgreements(missingParams);
        assertThat(missing.size(), is(0));
    }
}