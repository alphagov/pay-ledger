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
import uk.gov.pay.ledger.report.dao.PerformanceReportDao;
import uk.gov.pay.ledger.report.entity.GatewayAccountMonthlyPerformanceReportEntity;
import uk.gov.pay.ledger.report.entity.PerformanceReportEntity;
import uk.gov.pay.ledger.report.params.PerformanceReportParams.PerformanceReportParamsBuilder;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.transactionsummary.dao.TransactionSummaryDao;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Path("/v1/report")
@Produces(APPLICATION_JSON)
@Tag(name = "Reports")
public class PerformanceReportResource {

    private final TransactionSummaryDao transactionSummaryDao;
    private final PerformanceReportDao performanceReportDao;

    @Inject
    public PerformanceReportResource(TransactionSummaryDao transactionSummaryDao, PerformanceReportDao performanceReportDao) {
        this.transactionSummaryDao = transactionSummaryDao;
        this.performanceReportDao = performanceReportDao;
    }

    @Path("/performance-report")
    @GET
    @Timed
    @Operation(
            summary = "Get platform performance report (total volume and total_amount) for the date range and transaction state. Queries transaction_summary table for stats",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PerformanceReportEntity.class))),
                    @ApiResponse(responseCode = "400", description = "For missing query params or invalid state).", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error for invalid query params (from_date/to_date) values")
            }
    )
    public PerformanceReportEntity getPerformanceReport(@Parameter(description = "From date of the transaction summary to be searched (this date is inclusive). Required when to_date is provided", example = "2022-03-29")
                                                        @QueryParam("from_date") String fromDate,
                                                        @Parameter(description = "To date of the transaction summary to be searched (this date is inclusive). Required when from_date is provided", example = "2022-03-29")
                                                        @QueryParam("to_date") String toDate,
                                                        @Parameter(description = "Transaction state", schema = @Schema(implementation = TransactionState.class))
                                                        @QueryParam("state") String state) {

        var paramsBuilder = PerformanceReportParamsBuilder.builder();
        addDateRangeParamsOrThrow(paramsBuilder, fromDate, toDate);
        addStateParamOrThrow(paramsBuilder, state);
        return transactionSummaryDao.performanceReportForPaymentTransactions(paramsBuilder.build());
    }

    // expose aggregate data based on the transaction projection table to meet the need of
    // consumers that require information on terminal and in-flight payments (live payments dashboard)
    @Path("/performance-report-legacy")
    @GET
    @Timed
    @Operation(
            summary = "Get platform performance report (total volume and total_amount) for the date range and transaction state. Queries transaction table for stats, so could be slow for large date ranges",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PerformanceReportEntity.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error for invalid query parameter values")
            }
    )
    public PerformanceReportEntity getLegacyPerformanceReport(@Parameter(description = "From date of transactions to be searched (this date is inclusive).", example = "2022-03-29T01:00:00Z")
                                                              @QueryParam("from_date") String fromDate,
                                                              @Parameter(description = "To date of transactions to be searched (this date is exclusive).", example = "2022-03-29T01:00:00Z")
                                                              @QueryParam("to_date") String toDate,
                                                              @Parameter(description = "Transaction state", schema = @Schema(implementation = TransactionState.class))
                                                              @QueryParam("state") String state) {

        return performanceReportDao.performanceReportForPaymentTransactions(fromDate, toDate, state);
    }

    private void addStateParamOrThrow(PerformanceReportParamsBuilder builder, String state) {
        if (isNotBlank(state)) {
            Optional.ofNullable(TransactionState.from(state))
                    .map(builder::withState)
                    .orElseThrow(() -> new ValidationException("State provided must be one of uk.gov.pay.ledger.transaction.state.TransactionState"));
        }
    }

    private void addDateRangeParamsOrThrow(PerformanceReportParamsBuilder builder,
                                           String fromDate,
                                           String toDate) {
        boolean dateParamsProvided = isNotBlank(fromDate) || isNotBlank(toDate);
        if (dateParamsProvided) {
            if (isBlank(fromDate) || isBlank(toDate)) {
                throw new ValidationException("Both from_date and to_date must be provided");
            } else {
                builder.withFromDate(LocalDate.parse(fromDate)).withToDate(LocalDate.parse(toDate));
            }
        }
    }

    @Path("/gateway-performance-report")
    @GET
    @Timed
    @Operation(
            summary = "Get monthly performance report by gateway account. Queries transaction_summary table",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = GatewayAccountMonthlyPerformanceReportEntity.class)))),
                    @ApiResponse(responseCode = "400", description = "Missing required query parameters (from_date or to_date)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error for invalid query parameter values")
            }
    )
    public List<GatewayAccountMonthlyPerformanceReportEntity> getGatewayMonthlyPerformanceReport(
            @Parameter(description = "From date of transaction summary to be searched (this date is inclusive).", example = "2022-03-29", required = true)
            @QueryParam("from_date") String fromDate,
            @Parameter(description = "To date of transaction summary to be searched (this date is inclusive).", example = "2022-03-29", required = true)
            @QueryParam("to_date") String toDate) {
        if (isBlank(fromDate) || isBlank(toDate)) {
            throw new ValidationException("Both from_date and to_date must be provided");
        } else if (LocalDate.parse(fromDate).isAfter(LocalDate.parse(toDate))) {
            throw new ValidationException("from_date must be earlier or equal to to_date");
        }

        return transactionSummaryDao.monthlyPerformanceReportForGatewayAccounts(LocalDate.parse(fromDate), LocalDate.parse(toDate));
    }
}
