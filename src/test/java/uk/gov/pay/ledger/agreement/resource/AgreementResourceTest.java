package uk.gov.pay.ledger.agreement.resource;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.agreement.entity.AgreementEntity;
import uk.gov.pay.ledger.agreement.model.AgreementSearchResponse;
import uk.gov.pay.ledger.agreement.service.AgreementService;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.exception.BadRequestExceptionMapper;
import uk.gov.pay.ledger.exception.EmptyEventsException;
import uk.gov.pay.ledger.exception.JerseyViolationExceptionMapper;

import javax.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.agreement.AgreementEntityBuilder.anAgreementEntityWithId;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(MockitoExtension.class)
class AgreementResourceTest {

    private static final AgreementService agreementService = mock(AgreementService.class);
    private static final EventService eventService = mock(EventService.class);

    public static final ResourceExtension resources = ResourceExtension.builder()
            .addResource(new AgreementResource(agreementService, eventService))
            .addProvider(BadRequestExceptionMapper.class)
            .addProvider(JerseyViolationExceptionMapper.class)
            .build();

    @BeforeEach
    public void setUp() {
        Mockito.reset(agreementService, eventService);
    }

    @Test
    public void searchShouldReturn422_WithMissingBothServiceIdAndGatewayAccountId() {
        Response response = resources
                .target("/v1/agreement")
                .request()
                .get();

        assertThat(response.getStatus(), is(422));
    }

    @Test
    public void searchShouldReturn200_WithValidParams() {
        when(agreementService.searchAgreements(any(), any())).thenReturn(new AgreementSearchResponse(0L, 0L, 0L, List.of()));
        Response response = resources
                .target("/v1/agreement")
                .queryParam("service_id", "a-valid-service-id")
                .request()
                .get();

        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void searchShouldReturn200_WithUpperAndLowercaseStatus() {
        when(agreementService.searchAgreements(any(), any())).thenReturn(new AgreementSearchResponse(0L, 0L, 0L, List.of()));
        var lowerResponse = resources
                .target("/v1/agreement")
                .queryParam("service_id", "a-valid-service-id")
                .queryParam("status", "created")
                .request()
                .get();
        var upperResponse= resources
                .target("/v1/agreement")
                .queryParam("service_id", "a-valid-service-id")
                .queryParam("status", "CREATED")
                .request()
                .get();

        assertThat(lowerResponse.getStatus(), is(200));
        assertThat(upperResponse.getStatus(), is(200));
    }

    @Test
    public void searchShouldReturn400_WithInvalidStatusParam() {
        Response response = resources
                .target("/v1/agreement")
                .queryParam("service_id", "a-valid-service-id")
                .queryParam("status", "NOT_A_STATUS")
                .request()
                .get();

        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void findShouldReturn404_IfMissing() {
        Response response = resources
                .target("/v1/agreement/missing-agreement-id")
                .request()
                .get();

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void findShouldReturnProjectionDirectlyIfConsistentNotProvided() {
        when(agreementService.findAgreementEntity("agreement-id"))
                .thenReturn(Optional.of(stubAgreement("agreement-id", 1)));
        var response = resources
                .target("/v1/agreement/agreement-id")
                .request()
                .get();
        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(Map.class).get("external_id"), is("agreement-id"));
        verifyNoInteractions(eventService);
    }
    @Test
    public void findShouldReturnProjectionDirectlyIfConsistentFalse() {
        when(agreementService.findAgreementEntity("agreement-id"))
                .thenReturn(Optional.of(stubAgreement("agreement-id", 1)));
        var response = resources
                .target("/v1/agreement/agreement-id")
                .request()
                .header("X-Consistent", false)
                .get();
        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(Map.class).get("external_id"), is("agreement-id"));
        verifyNoInteractions(eventService);
    }

    @Test
    public void findConsistentShouldReturnProjectionDirectlyIfThereAreNoNewEvents() {
        var resourceId = "agreement-id";
        when(agreementService.findAgreementEntity(resourceId))
                .thenReturn(Optional.of(stubAgreement(resourceId, 1)));
        when(eventService.getEventDigestForResource(resourceId))
                .thenReturn(stubEventDigest(resourceId, 1));
        var response = resources
                .target("/v1/agreement/" + resourceId)
                .request()
                .header("X-Consistent", true)
                .get();
        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(Map.class).get("external_id"), is(resourceId));
        verify(eventService).getEventDigestForResource(resourceId);
    }

    @Test
    public void findConsistentShouldReturnNewProjectionIfThereAreNewEvents() {
        var resourceId = "agreement-id";
        var agreement = stubAgreement(resourceId, 1);
        var eventDigest = stubEventDigest(resourceId, 2);
        when(agreementService.findAgreementEntity(resourceId))
                .thenReturn(Optional.of(agreement));
        when(eventService.getEventDigestForResource(resourceId))
                .thenReturn(eventDigest);
        when(agreementService.projectAgreement(eventDigest))
                .thenReturn(agreement);
        var response = resources
                .target("/v1/agreement/" + resourceId)
                .request()
                .header("X-Consistent", true)
                .get();
        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(Map.class).get("external_id"), is(resourceId));
        verify(eventService).getEventDigestForResource(resourceId);
        verify(agreementService).projectAgreement(eventDigest);
    }

    @Test
    public void findConsistentShouldReturnNewProjectionIfThereAreOnlyEventsAndNoProjection() {
        var resourceId = "agreement-id";
        var eventDigest = stubEventDigest(resourceId, 1);
        when(agreementService.findAgreementEntity(resourceId))
                .thenReturn(Optional.empty());
        when(eventService.getEventDigestForResource(resourceId))
                .thenReturn(eventDigest);
        when(agreementService.projectAgreement(eventDigest))
                .thenReturn(stubAgreement(resourceId, 1));
        var response = resources
                .target("/v1/agreement/" + resourceId)
                .request()
                .header("X-Consistent", true)
                .get();
        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(Map.class).get("external_id"), is(resourceId));
        verify(eventService).getEventDigestForResource(resourceId);
        verify(agreementService).projectAgreement(eventDigest);
    }

    @Test
    public void findConsistentShouldReturn404_IfMissing() {
        var resourceId = "agreement-id";
        when(agreementService.findAgreementEntity(resourceId))
                .thenReturn(Optional.empty());
        when(eventService.getEventDigestForResource(resourceId))
                .thenThrow(EmptyEventsException.class);
        var response = resources
                .target("/v1/agreement/" + resourceId)
                .request()
                .header("X-Consistent", true)
                .get();
        assertThat(response.getStatus(), is(404));
        verify(eventService).getEventDigestForResource(resourceId);
    }

    @Test
    public void findShouldApplyAccountIdFilterIfPresent() {
        var resourceId = "agreement-id";
        var accountId = "account-id";
        var agreementWithCorrectAccount = anAgreementEntityWithId(resourceId)
                .withAccountId(accountId)
                .build();
        var agreementWithOtherAccount = anAgreementEntityWithId(resourceId)
                .withAccountId("other-account-id")
                .build();
        var agreementWithNoAccount = anAgreementEntityWithId(resourceId)
                .build();
        when(agreementService.findAgreementEntity(resourceId))
                .thenReturn(
                        Optional.of(agreementWithCorrectAccount),
                        Optional.of(agreementWithOtherAccount),
                        Optional.of(agreementWithNoAccount)
                );

        Supplier<Response> runQuery = () -> resources
                .target("/v1/agreement/" + resourceId)
                .queryParam("account_id", accountId)
                .request()
                .header("X-Consistent", false)
                .get();
        var responseForCorrectAccount = runQuery.get();
        var responseForOtherAccount = runQuery.get();
        var responseForNoAccount = runQuery.get();

        assertThat(responseForCorrectAccount.getStatus(), is(200));
        assertThat(responseForCorrectAccount.readEntity(Map.class).get("external_id"), is(resourceId));
        assertThat(responseForOtherAccount.getStatus(), is(404));
        assertThat(responseForNoAccount.getStatus(), is(404));
    }

    @Test
    public void findShouldApplyServiceIdFilterIfPresent() {
        var resourceId = "agreement-id";
        var serviceId = "service-id";
        var agreementWithCorrectService = anAgreementEntityWithId(resourceId)
                .withServiceId(serviceId)
                .build();
        var agreementWithOtherService = anAgreementEntityWithId(resourceId)
                .withServiceId("other-service-id")
                .build();
        var agreementWithNoService = anAgreementEntityWithId(resourceId)
                .build();
        when(agreementService.findAgreementEntity(resourceId))
                .thenReturn(
                        Optional.of(agreementWithCorrectService),
                        Optional.of(agreementWithOtherService),
                        Optional.of(agreementWithNoService)
                );

        Supplier<Response> runQuery = () -> resources
                .target("/v1/agreement/" + resourceId)
                .queryParam("service_id", serviceId)
                .request()
                .header("X-Consistent", false)
                .get();
        var responseForCorrectService = runQuery.get();
        var responseForOtherService = runQuery.get();
        var responseForNoService = runQuery.get();

        assertThat(responseForCorrectService.getStatus(), is(200));
        assertThat(responseForCorrectService.readEntity(Map.class).get("external_id"), is(resourceId));
        assertThat(responseForOtherService.getStatus(), is(404));
        assertThat(responseForNoService.getStatus(), is(404));
    }

    @Test
    public void findShouldApplyBothFiltersIfPresent() {
        var resourceId = "agreement-id";
        var accountId = "account-id";
        var serviceId = "service-id";
        var agreementWithCorrectAccountAndService = anAgreementEntityWithId(resourceId)
                .withAccountId(accountId)
                .withServiceId(serviceId)
                .build();
        var agreementWithOtherService = anAgreementEntityWithId(resourceId)
                .withAccountId(accountId)
                .withServiceId("other-service-id")
                .build();
        var agreementWithOtherAccount = anAgreementEntityWithId(resourceId)
                .withAccountId("other-account-id")
                .withServiceId(serviceId)
                .build();
        when(agreementService.findAgreementEntity(resourceId))
                .thenReturn(
                        Optional.of(agreementWithCorrectAccountAndService),
                        Optional.of(agreementWithOtherService),
                        Optional.of(agreementWithOtherAccount)
                );

        Supplier<Response> runQuery = () -> resources
                .target("/v1/agreement/" + resourceId)
                .queryParam("account_id", accountId)
                .queryParam("service_id", serviceId)
                .request()
                .header("X-Consistent", false)
                .get();
        var responseForCorrectAccountAndService = runQuery.get();
        var responseForOtherService = runQuery.get();
        var responseForOtherAccount = runQuery.get();

        assertThat(responseForCorrectAccountAndService.getStatus(), is(200));
        assertThat(responseForCorrectAccountAndService.readEntity(Map.class).get("external_id"), is(resourceId));
        assertThat(responseForOtherService.getStatus(), is(404));
        assertThat(responseForOtherAccount.getStatus(), is(404));
    }

    @Test
    public void findShouldApplyFiltersWhenConsistentTrueOrFalse() {
        var resourceId = "agreement-id";
        var accountId = "account-id";
        var serviceId = "service-id";
        var eventDigest1 = stubEventDigest(resourceId, 1);
        var eventDigest2 = stubEventDigest(resourceId, 2);
        var agreementMatchingFilters = anAgreementEntityWithId(resourceId)
                .withAccountId(accountId)
                .withServiceId(serviceId)
                .build();
        var agreementNotMatchingFilters = anAgreementEntityWithId(resourceId)
                .build();
        when(eventService.getEventDigestForResource(resourceId))
                .thenReturn(eventDigest1, eventDigest1, eventDigest2, eventDigest2);
        when(agreementService.projectAgreement(eventDigest2))
                .thenReturn(agreementNotMatchingFilters, agreementNotMatchingFilters);
        when(agreementService.findAgreementEntity(resourceId))
                .thenReturn(
                        Optional.of(agreementMatchingFilters),
                        Optional.of(agreementMatchingFilters),
                        Optional.of(agreementNotMatchingFilters),
                        Optional.of(agreementNotMatchingFilters)
                );

        Supplier<Response> runQueryConsistent = () -> resources
                .target("/v1/agreement/" + resourceId)
                .queryParam("account_id", accountId)
                .queryParam("service_id", serviceId)
                .request()
                .header("X-Consistent", true)
                .get();
        Supplier<Response> runQueryNotConsistent = () -> resources
                .target("/v1/agreement/" + resourceId)
                .queryParam("account_id", accountId)
                .queryParam("service_id", serviceId)
                .request()
                .header("X-Consistent", false)
                .get();
        var responseForConsistentMatchingFilters = runQueryConsistent.get();
        var responseForNotConsistentMatchingFilters = runQueryNotConsistent.get();
        var responseForConsistentNotMatchingFilters = runQueryConsistent.get();
        var responseForNotConsistentNotMatchingFilters = runQueryNotConsistent.get();

        assertThat(responseForConsistentMatchingFilters.getStatus(), is(200));
        assertThat(responseForConsistentMatchingFilters.readEntity(Map.class).get("external_id"), is(resourceId));
        assertThat(responseForNotConsistentMatchingFilters.getStatus(), is(200));
        assertThat(responseForNotConsistentMatchingFilters.readEntity(Map.class).get("external_id"), is(resourceId));
        assertThat(responseForConsistentNotMatchingFilters.getStatus(), is(404));
        assertThat(responseForNotConsistentNotMatchingFilters.getStatus(), is(404));
    }

    private AgreementEntity stubAgreement(String agreementId, Integer eventCount) {
        var agreementEntity = new AgreementEntity();
        agreementEntity.setExternalId(agreementId);
        agreementEntity.setEventCount(eventCount);
        return agreementEntity;
    }

    private EventDigest stubEventDigest(String agreementId, Integer eventCount) {
        return EventDigest.fromEventList(IntStream.range(0, eventCount)
                .mapToObj(i -> anEventFixture().withResourceExternalId(agreementId).toEntity())
                .collect(Collectors.toUnmodifiableList()));
    }
}