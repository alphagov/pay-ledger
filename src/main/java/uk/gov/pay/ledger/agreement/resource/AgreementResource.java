package uk.gov.pay.ledger.agreement.resource;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import uk.gov.pay.ledger.agreement.model.Agreement;
import uk.gov.pay.ledger.agreement.model.AgreementSearchResponse;
import uk.gov.pay.ledger.agreement.service.AgreementService;

import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/agreement")
@Produces(APPLICATION_JSON)
public class AgreementResource {
    private final AgreementService agreementService;

    @Inject
    public AgreementResource(AgreementService agreementService) {
        this.agreementService = agreementService;
    }

    @Path("/")
    @GET
    @Timed
    public AgreementSearchResponse search(@Valid @BeanParam AgreementSearchParams querySearchParams,
                                          @Context UriInfo uriInfo) {
        var searchParams = Optional.ofNullable(querySearchParams).orElse(new AgreementSearchParams());
        return agreementService.searchAgreements(searchParams, uriInfo);
    }

    @Path("/{agreementExternalId}")
    @GET
    @Timed
    public Agreement get(@PathParam("agreementExternalId") String agreementExternalId,
                         @Context UriInfo uriInfo) {
        return agreementService.findAgreement(agreementExternalId)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }
}