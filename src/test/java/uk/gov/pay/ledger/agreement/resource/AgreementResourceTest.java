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
import uk.gov.pay.ledger.exception.BadRequestExceptionMapper;
import uk.gov.pay.ledger.exception.JerseyViolationExceptionMapper;

import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(MockitoExtension.class)
class AgreementResourceTest {

    private static final AgreementService agreementService = mock(AgreementService.class);

    public static final ResourceExtension resources = ResourceExtension.builder()
            .addResource(new AgreementResource(agreementService))
            .addProvider(BadRequestExceptionMapper.class)
            .addProvider(JerseyViolationExceptionMapper.class)
            .build();

    @BeforeEach
    public void setUp() {
        Mockito.reset(agreementService);
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
    public void searchShouldReturn422_WithMissingBothServiceIdAndGatewayAccountId_WhenOverrideNotSpecified() {
        Response response = resources
                .target("/v1/agreement")
                .request()
                .get();

        assertThat(response.getStatus(), is(422));
    }

    @Test
    public void searchShouldReturn422_WithMissingBothServiceIdAndGatewayAccountId_WhenOverrideIsFalse() {
        Response response = resources
                .target("/v1/agreement")
                .queryParam("override_account_or_service_id_restriction", false)
                .request()
                .get();

        assertThat(response.getStatus(), is(422));
    }

    @Test
    public void searchShouldReturn200_WithMissingBothServiceIdAndGatewayAccountId_WhenOverrideIsTrue() {
        when(agreementService.searchAgreements(any(), any())).thenReturn(new AgreementSearchResponse(0L, 0L, 0L, List.of()));
        Response response = resources
                .target("/v1/agreement")
                .queryParam("override_account_or_service_id_restriction", true)
                .request()
                .get();

        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void findShouldReturn422_IfFiltersNotProvidedAndOverrideNotSpecified() {
        when(agreementService.findAgreementEntity("agreement-id", false, null, null))
                .thenReturn(Optional.of(stubAgreement("agreement-id", 1)));

        var response = resources
                .target("/v1/agreement/agreement-id")
                .request()
                .get();

        assertThat(response.getStatus(), is(422));
    }

    @Test
    public void findShouldReturn422_IfFiltersNotProvidedAndOverrideIsFalse() {
        when(agreementService.findAgreementEntity("agreement-id", false, null, null))
                .thenReturn(Optional.of(stubAgreement("agreement-id", 1)));

        var response = resources
                .target("/v1/agreement/agreement-id")
                .queryParam("override_account_or_service_id_restriction", false)
                .request()
                .get();

        assertThat(response.getStatus(), is(422));
    }

    @Test
    public void findShouldSucceedIfFiltersNotProvidedButOverrideIsTrue() {
        when(agreementService.findAgreementEntity("agreement-id", false, null, null))
                .thenReturn(Optional.of(stubAgreement("agreement-id", 1)));

        var response = resources
                .target("/v1/agreement/agreement-id")
                .queryParam("override_account_or_service_id_restriction", true)
                .request()
                .get();

        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void findShouldReturn404_IfMissing() {
        Response response = resources
                .target("/v1/agreement/missing-agreement-id")
                .queryParam("override_account_or_service_id_restriction", true)
                .request()
                .get();

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void findConsistentShouldReturn404_IfMissing() {
        var resourceId = "agreement-id";
        when(agreementService.findAgreementEntity(resourceId, true))
                .thenReturn(Optional.empty());
        var response = resources
                .target("/v1/agreement/" + resourceId)
                .queryParam("override_account_or_service_id_restriction", true)
                .request()
                .header("X-Consistent", true)
                .get();
        assertThat(response.getStatus(), is(404));
    }

    private AgreementEntity stubAgreement(String agreementId, Integer eventCount) {
        var agreementEntity = new AgreementEntity();
        agreementEntity.setExternalId(agreementId);
        agreementEntity.setEventCount(eventCount);
        return agreementEntity;
    }
}