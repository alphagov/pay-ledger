package uk.gov.pay.ledger.agreement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.agreement.dao.AgreementDao;
import uk.gov.pay.ledger.agreement.dao.PaymentInstrumentDao;
import uk.gov.pay.ledger.agreement.entity.AgreementEntity;
import uk.gov.pay.ledger.agreement.entity.AgreementsFactory;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.exception.EmptyEventsException;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;

@ExtendWith(MockitoExtension.class)
public class AgreementServiceTest {

    private static final AgreementDao agreementDao = mock(AgreementDao.class);
    private static final PaymentInstrumentDao paymentInstrumentDao = mock(PaymentInstrumentDao.class);
    private static final AgreementsFactory agreementEntityFactory = mock(AgreementsFactory.class);
    private static final EventService eventService = mock(EventService.class);

    private AgreementService agreementService;

    @BeforeEach
    public void setUp() {
        reset(agreementDao, paymentInstrumentDao, agreementEntityFactory, eventService);
        agreementService = new AgreementService(agreementDao, paymentInstrumentDao, agreementEntityFactory, eventService);
    }

    @Test
    public void findShouldReturnProjectionDirectlyIfConsistentFalse() {
        var resourceId = "agreement-id";
        var agreement = stubAgreement(resourceId, 1);
        when(agreementDao.findByExternalId(resourceId))
                .thenReturn(Optional.of(agreement));

        var result = agreementService.findAgreementEntity("agreement-id", false);

        assertThat(result, is(Optional.of(agreement)));
        verifyNoInteractions(eventService);
    }

    @Test
    public void findShouldReturnProjectionDirectlyIfConsistentTrueButThereAreNoNewEvents() {
        var resourceId = "agreement-id";
        var agreement = stubAgreement(resourceId, 1);
        when(eventService.getEventDigestForResource(resourceId))
                .thenReturn(stubEventDigest(resourceId, 1));
        when(agreementDao.findByExternalId(resourceId))
                .thenReturn(Optional.of(agreement));

        var result = agreementService.findAgreementEntity(resourceId, true);

        assertThat(result, is(Optional.of(agreement)));
    }

    @Test
    public void findShouldReturnNewProjectionIfConsistentTrueAndThereAreNewEvents() {
        var resourceId = "agreement-id";
        var agreement = stubAgreement(resourceId, 1);
        var eventDigest = stubEventDigest(resourceId, 2);
        when(eventService.getEventDigestForResource(resourceId))
                .thenReturn(eventDigest);
        when(agreementEntityFactory.create(eventDigest))
                .thenReturn(agreement);

        var result = agreementService.findAgreementEntity(resourceId, true);

        assertThat(result, is(Optional.of(agreement)));
    }

    @Test
    public void findShouldReturnNewProjectionIfConsistentTrueAndThereAreOnlyEventsAndNoProjection() {
        var resourceId = "agreement-id";
        var agreement = stubAgreement(resourceId, 1);
        var eventDigest = stubEventDigest(resourceId, 1);
        when(eventService.getEventDigestForResource(resourceId))
                .thenReturn(eventDigest);
        when(agreementEntityFactory.create(eventDigest))
                .thenReturn(agreement);

        var result = agreementService.findAgreementEntity(resourceId, true);

        assertThat(result, is(Optional.of(agreement)));
    }

    @Test
    public void findShouldReturnEmptyValueWhenEventServiceHasNoEvents() {
        var resourceId = "agreement-id";
        when(eventService.getEventDigestForResource(resourceId))
                .thenThrow(EmptyEventsException.class);

        var result = agreementService.findAgreementEntity(resourceId, true);

        assertThat(result, is(Optional.empty()));
    }

    @Test
    public void findShouldApplyFiltersWhenConsistentIsFalse() {
        var resourceId = "agreement-id";
        var accountId = "123";
        var serviceId = "service-id";
        var agreement = stubAgreement(resourceId, 1, accountId, serviceId);
        when(agreementDao.findByExternalId(resourceId))
                .thenReturn(Optional.of(agreement));

        BiFunction<String, String, Optional<AgreementEntity>> findAgreement = (String accountFilter, String serviceFilter) ->
                agreementService.findAgreementEntity(resourceId, false, accountFilter, serviceFilter);

        assertThat(findAgreement.apply(accountId, serviceId), is(Optional.of(agreement)));
        assertThat(findAgreement.apply(accountId, null), is(Optional.of(agreement)));
        assertThat(findAgreement.apply(null, serviceId), is(Optional.of(agreement)));
        assertThat(findAgreement.apply(null, null), is(Optional.of(agreement)));
        assertThat(findAgreement.apply("456", "other-service-id"), is(Optional.empty()));
        assertThat(findAgreement.apply(null, "other-service-id"), is(Optional.empty()));
        assertThat(findAgreement.apply("456", null), is(Optional.empty()));
        assertThat(findAgreement.apply(accountId, "other-service-id"), is(Optional.empty()));
        assertThat(findAgreement.apply("456", serviceId), is(Optional.empty()));
    }

    @Test
    public void findShouldApplyFiltersWhenConsistentIsTrue() {
        var resourceId = "agreement-id";
        var accountId = "123";
        var serviceId = "service-id";
        var agreement = stubAgreement(resourceId, 1, accountId, serviceId);
        when(eventService.getEventDigestForResource(resourceId))
                .thenReturn(stubEventDigest(resourceId, 1));
        when(agreementDao.findByExternalId(resourceId))
                .thenReturn(Optional.of(agreement));

        BiFunction<String, String, Optional<AgreementEntity>> findAgreement = (String accountFilter, String serviceFilter) ->
                agreementService.findAgreementEntity(resourceId, true, accountFilter, serviceFilter);

        assertThat(findAgreement.apply(accountId, serviceId), is(Optional.of(agreement)));
        assertThat(findAgreement.apply(accountId, null), is(Optional.of(agreement)));
        assertThat(findAgreement.apply(null, serviceId), is(Optional.of(agreement)));
        assertThat(findAgreement.apply(null, null), is(Optional.of(agreement)));
        assertThat(findAgreement.apply("456", "other-service-id"), is(Optional.empty()));
        assertThat(findAgreement.apply(null, "other-service-id"), is(Optional.empty()));
        assertThat(findAgreement.apply("456", null), is(Optional.empty()));
        assertThat(findAgreement.apply(accountId, "other-service-id"), is(Optional.empty()));
        assertThat(findAgreement.apply("456", serviceId), is(Optional.empty()));
    }

    private AgreementEntity stubAgreement(String agreementId, Integer eventCount) {
        var agreementEntity = new AgreementEntity();
        agreementEntity.setExternalId(agreementId);
        agreementEntity.setEventCount(eventCount);
        return agreementEntity;
    }

    private AgreementEntity stubAgreement(String agreementId, Integer eventCount, String accountId, String serviceId) {
        var agreementEntity = new AgreementEntity();
        agreementEntity.setExternalId(agreementId);
        agreementEntity.setEventCount(eventCount);
        agreementEntity.setGatewayAccountId(accountId);
        agreementEntity.setServiceId(serviceId);
        return agreementEntity;
    }

    private EventDigest stubEventDigest(String agreementId, Integer eventCount) {
        return EventDigest.fromEventList(IntStream.range(0, eventCount)
                .mapToObj(i -> anEventFixture().withResourceExternalId(agreementId).toEntity())
                .collect(Collectors.toUnmodifiableList()));
    }

}
