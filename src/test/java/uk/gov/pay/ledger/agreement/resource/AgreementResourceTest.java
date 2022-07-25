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
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.exception.BadRequestExceptionMapper;
import uk.gov.pay.ledger.exception.EmptyEventsException;
import uk.gov.pay.ledger.exception.JerseyViolationExceptionMapper;

import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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

    private AgreementEntity stubAgreement(String agreementId, Integer eventCount) {
        var agreementEntity = new AgreementEntity();
        agreementEntity.setExternalId(agreementId);
        agreementEntity.setEventCount(eventCount);
        return agreementEntity;
    }

    private EventDigest stubEventDigest(String agreementId, Integer eventCount) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < eventCount; i++) {
            events.add(anEventFixture().withResourceExternalId(agreementId).toEntity());
        }
        return EventDigest.fromEventList(events);
    }
}