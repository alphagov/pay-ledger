package uk.gov.pay.ledger.agreement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.agreement.dao.AgreementDao;
import uk.gov.pay.ledger.agreement.dao.PaymentInstrumentDao;
import uk.gov.pay.ledger.agreement.entity.AgreementEntity;
import uk.gov.pay.ledger.agreement.resource.AgreementSearchParams;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.util.fixture.AgreementFixture;
import uk.gov.pay.ledger.util.fixture.EventFixture;
import uk.gov.pay.ledger.util.fixture.PaymentInstrumentFixture;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;

class AgreementDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private final AgreementDao agreementDao = new AgreementDao(rule.getJdbi());
    private final PaymentInstrumentDao paymentInstrumentDao = new PaymentInstrumentDao(rule.getJdbi());
    private final EventDao eventDao = rule.getJdbi().onDemand(EventDao.class);

    @AfterEach
    void tearDown() {
        aDatabaseTestHelper(rule.getJdbi()).truncateAllData();
    }

    @Test
    void shouldInsertAgreement() {
        AgreementFixture fixture = AgreementFixture.anAgreementFixture().withUserIdentifier("a-valid-user-identifier");
        agreementDao.upsert(fixture.toEntity());

        AgreementEntity fetchedEntity = agreementDao.findByExternalId(fixture.getExternalId()).get();

        assertThat(fetchedEntity, is(notNullValue()));
        assertThat(fetchedEntity.getExternalId(), is(fixture.getExternalId()));
        assertThat(fetchedEntity.getUserIdentifier(), is("a-valid-user-identifier"));
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
        var paymentInstrumentFixtureThree = PaymentInstrumentFixture.aPaymentInstrumentFixture("aac", agreementFixture.getExternalId(), ZonedDateTime.now(ZoneOffset.UTC).minusDays(5));
        agreementDao.upsert(agreementFixture.toEntity());
        paymentInstrumentDao.upsert(paymentInstrumentFixtureOne.toEntity());
        paymentInstrumentDao.upsert(paymentInstrumentFixtureTwo.toEntity());
        paymentInstrumentDao.upsert(paymentInstrumentFixtureThree.toEntity());

        AgreementEntity fetchedEntity = agreementDao.findByExternalId(agreementFixture.getExternalId()).get();
        assertThat(fetchedEntity.getPaymentInstrument().getExternalId(), is(paymentInstrumentFixtureTwo.getExternalId()));
    }

    @Test
    void shouldSearchAgreement() {
        var fixture = AgreementFixture.anAgreementFixture()
                .withExternalId("external-id")
                .withServiceId("service-id");
        var secondFixture = AgreementFixture.anAgreementFixture()
                .withExternalId("second-external-id")
                .withServiceId("second-service-id");
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

    @Test
    void shouldFindAssociatedEventsForAnAgreementAndItsPaymentInstruments() {
        var serviceId = "service-id";
        var agreementId = "agreement-id";
        var differentAgreementId = "different-agreement-id";
        var oldPaymentInstrumentId = "old-payment-instrument-id";
        var newPaymentInstrumentId = "new-payment-instrument-id";
        var differentPaymentInstrumentId = "etc-payment-instrument-id";
        
        var agreementFixture = AgreementFixture.anAgreementFixture()
                .withExternalId(agreementId)
                .withServiceId(serviceId);
        var differentAgreementFixture = AgreementFixture.anAgreementFixture()
                .withExternalId(differentAgreementId)
                .withServiceId(serviceId);
        agreementDao.upsert(agreementFixture.toEntity());
        agreementDao.upsert(differentAgreementFixture.toEntity());
        
        var oldPaymentInstrument = PaymentInstrumentFixture.aPaymentInstrumentFixture()
                .withExternalId(oldPaymentInstrumentId)
                .withAgreementExternalId(agreementId);
        var newPaymentInstrument = PaymentInstrumentFixture.aPaymentInstrumentFixture()
                .withExternalId(newPaymentInstrumentId)
                .withAgreementExternalId(agreementId);
        var differentPaymentInstrument = PaymentInstrumentFixture.aPaymentInstrumentFixture()
                .withExternalId(differentPaymentInstrumentId)
                .withAgreementExternalId(differentAgreementId);
        paymentInstrumentDao.upsert(oldPaymentInstrument.toEntity());
        paymentInstrumentDao.upsert(newPaymentInstrument.toEntity());
        paymentInstrumentDao.upsert(differentPaymentInstrument.toEntity());

        var agreementEvent1 = EventFixture.anEventFixture()
                .withResourceExternalId(agreementId)
                .withServiceId(serviceId)
                .withResourceType(ResourceType.AGREEMENT)
                .withEventType("CREATED")
                .withEventData("{\"data\": \"Event 1\"}");
        var agreementEvent2 = EventFixture.anEventFixture()
                .withResourceExternalId(agreementId)
                .withServiceId(serviceId)
                .withResourceType(ResourceType.AGREEMENT)
                .withEventType("AGREEMENT_SET_UP")
                .withEventData("{\"data\": \"Event 2\"}");
        var differentAgreementEvent = EventFixture.anEventFixture()
                .withResourceExternalId(differentAgreementId)
                .withServiceId(serviceId)
                .withResourceType(ResourceType.AGREEMENT)
                .withEventType("CREATED")
                .withEventData("{\"data\": \"Event 3\"}");
        var oldPaymentInstrumentEvent = EventFixture.anEventFixture()
                .withResourceExternalId(agreementId)
                .withServiceId(serviceId)
                .withResourceType(ResourceType.PAYMENT_INSTRUMENT)
                .withEventType("CREATED")
                .withEventData("{\"data\": \"Event 4\"}");
        var newPaymentInstrumentEvent = EventFixture.anEventFixture()
                .withResourceExternalId(agreementId)
                .withServiceId(serviceId)
                .withResourceType(ResourceType.PAYMENT_INSTRUMENT)
                .withEventType("CREATED")
                .withEventData("{\"data\": \"Event 5\"}");
        var differentPaymentInstrumentEvent = EventFixture.anEventFixture()
                .withResourceExternalId(differentAgreementId)
                .withServiceId(serviceId)
                .withResourceType(ResourceType.PAYMENT_INSTRUMENT)
                .withEventType("CREATED")
                .withEventData("{\"data\": \"Event 6\"}");
        eventDao.insertEventWithResourceTypeId(agreementEvent1.toEntity());
        eventDao.insertEventWithResourceTypeId(agreementEvent2.toEntity());
        eventDao.insertEventWithResourceTypeId(differentAgreementEvent.toEntity());
        eventDao.insertEventWithResourceTypeId(oldPaymentInstrumentEvent.toEntity());
        eventDao.insertEventWithResourceTypeId(newPaymentInstrumentEvent.toEntity());
        eventDao.insertEventWithResourceTypeId(differentPaymentInstrumentEvent.toEntity());
        
        var results = agreementDao.findAssociatedEvents(agreementId);
        
        assertThat(results.size(), is(4));
        assertThat(results.get(0).getEventData(), is("{\"data\": \"Event 1\"}"));
        assertThat(results.get(1).getEventData(), is("{\"data\": \"Event 2\"}"));
        assertThat(results.get(2).getEventData(), is("{\"data\": \"Event 4\"}"));
        assertThat(results.get(3).getEventData(), is("{\"data\": \"Event 5\"}"));
    }
}
