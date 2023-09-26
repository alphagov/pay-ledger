package uk.gov.pay.ledger.expungeorredact.resource;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.gov.pay.ledger.expungeorredact.service.ExpungeOrRedactService;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/v1/tasks")
@Produces("application/json")
@Tag(name = "Tasks")
public class ExpungeOrRedactResource {

    private final ExpungeOrRedactService expungeOrRedactService;

    @Inject
    public ExpungeOrRedactResource(ExpungeOrRedactService expungeOrRedactService) {
        this.expungeOrRedactService = expungeOrRedactService;
    }

    @Path("/expunge-or-redact-historical-data")
    @POST
    @Timed
    @Operation(
            summary = "Redacts/Removes historical data based on `expungeOrRedactHistoricalDataConfig`. Currently redacts PII from transactions and deletes related transaction events",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
            }
    )
    public Response expungeOrRedactData() {
        expungeOrRedactService.redactOrDeleteData();
        return Response.ok().build();
    }
}
