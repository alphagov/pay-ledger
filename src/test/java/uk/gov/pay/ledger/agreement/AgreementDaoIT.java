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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class AgreementDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private final AgreementDao agreementDao = new AgreementDao(rule.getJdbi());
    private final PaymentInstrumentDao paymentInstrumentDao = new PaymentInstrumentDao(rule.getJdbi());

    @Test
    void shouldInsertAgreement() {
        AgreementFixture fixture = AgreementFixture.anAgreementFixture();
        fixture.setUserIdentifier("an-valid-user-identifier");
        agreementDao.upsert(fixture.toEntity());

        AgreementEntity fetchedEntity = agreementDao.findByExternalId(fixture.getExternalId()).get();

        assertThat(fetchedEntity, is(notNullValue()));
        assertThat(fetchedEntity.getExternalId(), is(fixture.getExternalId()));
        assertThat(fetchedEntity.getUserIdentifier(), is("an-valid-user-identifier"));
    }

    @Test
    void shouldFindAgreementByExternalId() {
        AgreementFixture fixture = AgreementFixture.anAgreementFixture();
        agreementDao.upsert(fixture.toEntity());

        AgreementEntity fetchedEntity = agreementDao.findByExternalId(fixture.getExternalId()).get();

        // assert fields are appropriately set, re-constructed and correctly typed by the agreement mapper
        assertThat(fetchedEntity.getExternalId(), is(fixture.getExternalId()));
        assertThat(fetchedEntity.getDescription(), is(fixture.getDescription()));
        assertThat(fetchedEntity.getCreatedDate(), is(fixture.getCreatedDate()));
    }

    @Test
    void shouldCorrectlyMapAndFindAgreementWithPaymentInstrumentByExternalId() {
        var paymentInstrumentExternalId = "pi-external-id";
        AgreementFixture agreementFixture = AgreementFixture.anAgreementFixture();
        var paymentInstrumentFixture = PaymentInstrumentFixture.aPaymentInstrumentFixture(
                paymentInstrumentExternalId,
                agreementFixture.getExternalId(),
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        agreementDao.upsert(agreementFixture.toEntity());
        paymentInstrumentDao.upsert(paymentInstrumentFixture.toEntity());

        AgreementEntity fetchedEntity = agreementDao.findByExternalId(agreementFixture.getExternalId()).get();

        assertThat(fetchedEntity.getExternalId(), is(agreementFixture.getExternalId()));

        // assert payment instrument fields are appropriately joined and typed by the agreement mapper
        assertThat(fetchedEntity.getPaymentInstrument().getExternalId(), is(paymentInstrumentExternalId));
        assertThat(fetchedEntity.getPaymentInstrument().getCreatedDate(), is(paymentInstrumentFixture.getCreatedDate()));
        assertThat(fetchedEntity.getPaymentInstrument().getAgreementExternalId(), is(agreementFixture.getExternalId()));
    }

    @Test
    void shouldFindAndGuaranteeMostRecentlyCreatedPaymentInstrumentByExternalId() {
        AgreementFixture agreementFixture = AgreementFixture.anAgreementFixture();
        var paymentInstrumentFixtureOne = PaymentInstrumentFixture.aPaymentInstrumentFixture("aaa", agreementFixture.getExternalId(), ZonedDateTime.now(ZoneOffset.UTC).minusDays(10));
        var paymentInstrumentFixtureTwo = PaymentInstrumentFixture.aPaymentInstrumentFixture("aab", agreementFixture.getExternalId(), ZonedDateTime.now(ZoneOffset.UTC));
        var paymentInstrumentFixtureThree= PaymentInstrumentFixture.aPaymentInstrumentFixture("aac", agreementFixture.getExternalId(), ZonedDateTime.now(ZoneOffset.UTC).minusDays(5));
        agreementDao.upsert(agreementFixture.toEntity());
        paymentInstrumentDao.upsert(paymentInstrumentFixtureOne.toEntity());
        paymentInstrumentDao.upsert(paymentInstrumentFixtureTwo.toEntity());
        paymentInstrumentDao.upsert(paymentInstrumentFixtureThree.toEntity());

        AgreementEntity fetchedEntity = agreementDao.findByExternalId(agreementFixture.getExternalId()).get();
        assertThat(fetchedEntity.getPaymentInstrument().getExternalId(), is(paymentInstrumentFixtureTwo.getExternalId()));
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