package uk.gov.pay.ledger.report.resource;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.gov.pay.ledger.exception.ErrorResponse;
import uk.gov.pay.ledger.exception.ValidationException;
import uk.gov.pay.ledger.report.entity.TimeseriesReportSlice;
import uk.gov.pay.ledger.report.entity.TransactionSummaryResult;
import uk.gov.pay.ledger.report.params.TransactionSummaryParams;
import uk.gov.pay.ledger.report.service.ReportService;
import uk.gov.pay.ledger.report.validator.TransactionSummaryValidator;
import uk.gov.pay.ledger.transaction.service.AccountIdSupplierManager;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Path("/v1/report")
@Produces(APPLICATION_JSON)
@Tag(name = "Reports")
public class ReportResource {

    private final ReportService reportService;

    @Inject
    public ReportResource(ReportService reportService) {
        this.reportService = reportService;
    }

    @Path("/payments_by_state")
    @GET
    @Timed
    @Operation(
            summary = "Get number of payments by transaction (payment) state",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(type = "object", example = "{" +
                            "    \"timedout\": 0," +
                            "    \"submitted\": 0," +
                            "    \"declined\": 0," +
                            "    \"created\": 0," +
                            "    \"success\": 1," +
                            "    \"cancelled\": 0," +
                            "    \"started\": 0," +
                            "    \"error\": 0," +
                            "    \"undefined\": 0," +
                            "    \"capturable\": 0" +
                            "}"))),
                    @ApiResponse(responseCode = "422", description = "Missing required or invalid query parameters (from_date or to_date)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response getPaymentCountsByState(
            @Valid @BeanParam TransactionSummaryParams transactionSummaryParams,
            @Parameter(description = "Set to true to get counts for all gateway accounts.")
            @QueryParam("override_account_id_restriction") Boolean overrideAccountRestriction) {

        AccountIdSupplierManager<Map<String, Long>> accountIdSupplierManager =
                AccountIdSupplierManager.of(overrideAccountRestriction, transactionSummaryParams.getAccountId());

        Map<String, Long> paymentCountsByState = accountIdSupplierManager
                .withSupplier(accountId -> reportService.getPaymentCountsByState(transactionSummaryParams))
                .withPrivilegedSupplier(() -> reportService.getPaymentCountsByState(transactionSummaryParams))
                .validateAndGet();

        return Response.status(OK).entity(paymentCountsByState).build();
    }

    @Path("/transactions-summary")
    @GET
    @Timed
    @Operation(
            summary = "Get transaction summary for query params",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = TransactionSummaryResult.class))),
                    @ApiResponse(responseCode = "400", description = "Missing required query parameter (from_date)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "422", description = "Missing required or invalid query parameters (from_date or to_date)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public TransactionSummaryResult getTransactionSummaryResult(
            @Valid @BeanParam TransactionSummaryParams transactionSummaryParams,
            @Parameter(description = "Set to true to return summary for all accounts.")
            @QueryParam("override_account_id_restriction") Boolean overrideAccountRestriction,
            @Parameter(description = "Set to true to make from_date non-mandatory.")
            @QueryParam("override_from_date_validation") @DefaultValue("false") Boolean overrideFromDateValidation) {

        TransactionSummaryValidator.validateTransactionSummaryParams(transactionSummaryParams, overrideFromDateValidation);

        AccountIdSupplierManager<TransactionSummaryResult> accountIdSupplierManager =
                AccountIdSupplierManager.of(overrideAccountRestriction, transactionSummaryParams.getAccountId());

        return accountIdSupplierManager
                .withSupplier(accountId -> reportService.getTransactionsSummary(transactionSummaryParams))
                .withPrivilegedSupplier(() -> reportService.getTransactionsSummary(transactionSummaryParams))
                .validateAndGet();
    }

    @Path("/transactions-by-hour")
    @GET
    @Timed
    @Operation(
            summary = "Get transaction summary by hour",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TimeseriesReportSlice.class)))),
                    @ApiResponse(responseCode = "400", description = "Missing required query parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Invalid query parameters (from_date or to_date)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public List<TimeseriesReportSlice> getTransactionsByHour(
            @Parameter(description = "From date of transaction summary to be searched (this date is inclusive).", example = "2022-03-29T00:00:00Z", required = true)
            @QueryParam("from_date") String fromDate,
            @Parameter(description = "To date of transaction summary to be searched (this date is inclusive).", example = "2022-03-29T00:00:00Z", required = true)
            @QueryParam("to_date") String toDate
    ) {
        if (isBlank(fromDate) || isBlank(toDate)) {
            throw new ValidationException("from_date and to_date must be specified");
        }
        return reportService.getTransactionsByHour(
                ZonedDateTime.parse(fromDate),
                ZonedDateTime.parse(toDate)
        );
    }
}
