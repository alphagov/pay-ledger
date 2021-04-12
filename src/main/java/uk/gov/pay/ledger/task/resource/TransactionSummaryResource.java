package uk.gov.pay.ledger.task.resource;

import uk.gov.pay.ledger.report.service.ReportService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;

@Path("/")
public class TransactionSummaryResource {

    private ReportService reportService;

    @Inject
    public TransactionSummaryResource(ReportService reportService) {
        this.reportService = reportService;
    }

    @GET
    @Path("/v1/tasks/update-transaction-summary")
    @Produces(APPLICATION_JSON)
    public Response expunge() {
        reportService.updateTransactionSummary();
        return status(OK).build();
    }
}
