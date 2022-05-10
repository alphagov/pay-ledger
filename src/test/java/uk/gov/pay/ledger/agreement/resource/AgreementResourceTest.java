package uk.gov.pay.ledger.agreement.resource;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.agreement.model.AgreementSearchResponse;
import uk.gov.pay.ledger.agreement.service.AgreementService;
import uk.gov.pay.ledger.exception.BadRequestExceptionMapper;
import uk.gov.pay.ledger.exception.JerseyViolationExceptionMapper;

import javax.ws.rs.core.Response;

import java.util.List;

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
}