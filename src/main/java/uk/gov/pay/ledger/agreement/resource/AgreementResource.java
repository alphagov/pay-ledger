package uk.gov.pay.ledger.agreement.resource;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.gov.pay.ledger.agreement.model.Agreement;
import uk.gov.pay.ledger.agreement.model.AgreementEventsResponse;
import uk.gov.pay.ledger.agreement.model.AgreementSearchResponse;
import uk.gov.pay.ledger.agreement.service.AgreementService;

import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static uk.gov.pay.ledger.common.consistent.ConsistentKeys.HEADER_PARAM_X_CONSISTENT;

@Path("/v1/agreement")
@Produces(APPLICATION_JSON)
@Tag(name = "Agreements")
public class AgreementResource {
    private final AgreementService agreementService;

    @Inject
    public AgreementResource(AgreementService agreementService) {
        this.agreementService = agreementService;
    }

    @Path("/")
    @GET
    @Timed
    @Operation(
            summary = "Search agreements by query params",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AgreementSearchResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public AgreementSearchResponse search(@Valid
                                          @BeanParam AgreementSearchParams querySearchParams,
                                          @Context UriInfo uriInfo) {
        var searchParams = Optional.ofNullable(querySearchParams).orElse(new AgreementSearchParams());
        return agreementService.searchAgreements(searchParams, uriInfo);
    }

    @Path("/{agreementExternalId}")
    @GET
    @Timed
    @Operation(
            summary = "Find agreement by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Agreement.class))),
                    @ApiResponse(responseCode = "404", description = "Not found"),
                    @ApiResponse(responseCode = "422", description = "Invalid parameters. " +
                            "One of service_id or account_id fields is required unless override_account_or_service_id_restriction=true")
            }
    )
    public Agreement get(@Parameter(description = "The unique external id for the agreement", example = "cgc1ocvh0pt9fqs0ma67r42l58")
                         @PathParam("agreementExternalId") String agreementExternalId,
                         @Parameter(description = "If true, an additional check will be carried out to ensure that ledger database is up-to-date with latest events before responding", example = "true")
                         @HeaderParam(HEADER_PARAM_X_CONSISTENT) Boolean isConsistent,
                         @Parameter(description = "The gateway account id linked to the agreement", example = "1")
                         @QueryParam("account_id") String accountId,
                         @Parameter(description = "The service id linked to the agreement", example = "1")
                         @QueryParam("service_id") String serviceId,
                         @Parameter(description = "If false, the account_id or service_id must be specified", example = "false")
                         @QueryParam("override_account_or_service_id_restriction") Boolean overrideFilterRestrictions,
                         @Context UriInfo uriInfo) {
        if (!Boolean.TRUE.equals(overrideFilterRestrictions) && accountId == null && serviceId == null) {
            throw new WebApplicationException("One of [service_id] or [account_id] fields is required", 422);
        }

        return agreementService.findAgreementEntity(agreementExternalId, Boolean.TRUE.equals(isConsistent), accountId, serviceId)
                .map(Agreement::from)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    @Path("/{agreementExternalId}/event")
    @GET
    @Timed
    @Operation(
            summary = "Find agreement events by agreement ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AgreementEventsResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public AgreementEventsResponse getEvents(@Parameter(description = "The unique external id for the agreement", example = "cgc1ocvh0pt9fqs0ma67r42l58")
                                 @PathParam("agreementExternalId") String agreementExternalId,
                                 @Parameter(description = "The service id linked to the agreement", example = "1")
                                 @QueryParam("service_id") String serviceId,
                                 @Context UriInfo uriInfo) {
        return new AgreementEventsResponse(agreementService.findEvents(agreementExternalId, serviceId));
    }
}
