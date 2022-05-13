package uk.gov.pay.ledger.transaction.resource;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jersey.repackaged.com.google.common.base.Stopwatch;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.exception.ErrorResponse;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.TransactionEventResponse;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.model.TransactionsForTransactionResponse;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.transaction.service.AccountIdListSupplierManager;
import uk.gov.pay.ledger.transaction.service.AccountIdSupplierManager;
import uk.gov.pay.ledger.transaction.service.CsvService;
import uk.gov.pay.ledger.transaction.service.TransactionService;
import uk.gov.pay.ledger.util.CommaDelimitedSetParameter;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.pay.ledger.transaction.search.common.TransactionSearchParamsValidator.validateSearchParams;
import static uk.gov.pay.ledger.transaction.search.common.TransactionSearchParamsValidator.validateSearchParamsForCsv;

@Path("/v1/transaction")
@Produces("application/json; qs=1")
@Tag(name = "Transactions")
public class TransactionResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionResource.class);
    private static final String ACCOUNT_MANAGER_FIELD_NAME = "account_id";
    private static final String SQL_PROCESSING_WAS_INTERRUPTED_BY_A_CANCEL_REQUEST_FROM_A_CLIENT_PROGRAM_STATE_CODE = "57014";
    private final TransactionService transactionService;
    private final CsvService csvService;
    private final LedgerConfig configuration;

    @Inject
    public TransactionResource(TransactionService transactionService, CsvService csvService, LedgerConfig configuration) {
        this.transactionService = transactionService;
        this.csvService = csvService;
        this.configuration = configuration;
    }

    @Path("/{transactionExternalId}")
    @GET
    @Timed
    @Operation(
            summary = "Get transaction by external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionView.class)))),
                    @ApiResponse(responseCode = "400", description = "Missing required query parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error for invalid query parameter values")
            }
    )
    public TransactionView getById(@PathParam("transactionExternalId") String transactionExternalId,
                                   @QueryParam("account_id") String gatewayAccountId,
                                   @QueryParam("override_account_id_restriction") Boolean overrideAccountRestriction,
                                   @QueryParam("transaction_type") TransactionType transactionType,
                                   @QueryParam("parent_external_id") String parentTransactionExternalId,
                                   @DefaultValue("2") @QueryParam("status_version") int statusVersion
    ) {
        LOGGER.info("Get transaction request: {}", transactionExternalId);

        AccountIdSupplierManager<Optional<TransactionView>> accountIdSupplierManager =
                AccountIdSupplierManager.of(overrideAccountRestriction, gatewayAccountId);

        return accountIdSupplierManager
                .withSupplier((accountId) -> transactionService.getTransactionForGatewayAccount(accountId,
                        transactionExternalId, transactionType,
                        parentTransactionExternalId, statusVersion))
                .withPrivilegedSupplier(() -> transactionService.getTransaction(transactionExternalId, statusVersion))
                .validateAndGet()
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    @Path("/")
    @GET
    @Timed
    @Operation(
            summary = "Search transactions by query params. Same endpoint can be used to download CSV (with  Accept header=\"text/csv\"). Refer to code for details",
            operationId = "search transactions",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionSearchResponse.class)),  mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Missing required query parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "504", description = "Search query cancelled due to query timeout")
            }
    )
    public TransactionSearchResponse search(@Valid
                                            @Parameter(schema = @Schema(implementation = TransactionSearchParams.class))
                                            @BeanParam TransactionSearchParams searchParams,
                                            @Parameter(description = "Set to true to list transactions for all accounts.")
                                            @QueryParam("override_account_id_restriction") Boolean overrideAccountRestriction,
                                            @Parameter(description = "Comma delimited gateway account IDs. Required except when override_account_id_restriction=true", example = "1,2", schema = @Schema(type = "string", implementation = String.class))
                                            @QueryParam("account_id") CommaDelimitedSetParameter gatewayAccountIds,
                                            @Context UriInfo uriInfo) {
        try {
            return searchForTransactions(searchParams, overrideAccountRestriction, gatewayAccountIds, uriInfo);
        } catch (UnableToExecuteStatementException e) {
            if (e.getCause() instanceof SQLException) {
                if (((SQLException) e.getCause()).getSQLState().equals(SQL_PROCESSING_WAS_INTERRUPTED_BY_A_CANCEL_REQUEST_FROM_A_CLIENT_PROGRAM_STATE_CODE)) {
                    // a query specified timeout was reached
                    LOGGER.warn("Search query cancelled by client query timeout");
                    throw new WebApplicationException("could not get the requested page", Response.Status.GATEWAY_TIMEOUT);
                }
            }
            throw new WebApplicationException();
        }
    }

    @Path("/")
    @GET
    @Produces("text/csv; qs=.5")
    @Timed
    @Hidden
    public Response streamCsv(@Valid @BeanParam TransactionSearchParams searchParams,
                              @QueryParam("account_id") CommaDelimitedSetParameter gatewayAccountIds,
                              @QueryParam("fee_headers") boolean includeFeeHeaders,
                              @QueryParam("moto_header") boolean includeMotoHeader,
                              @Context UriInfo uriInfo) {
        StreamingOutput stream = outputStream -> {
            TransactionSearchParams csvSearchParams = Optional.ofNullable(searchParams).orElse(new TransactionSearchParams());

            validateSearchParamsForCsv(csvSearchParams, gatewayAccountIds);

            csvSearchParams.overrideMaxDisplaySize((long) configuration.getReportingConfig().getStreamingCsvPageSize());
            csvSearchParams.setAccountIds(gatewayAccountIds.getParameters());

            List<TransactionEntity> page;
            ZonedDateTime startingAfterCreatedDate = null;
            Long startingAfterId = null;
            int count = 0;

            Map<String, Object> headers = csvService.csvHeaderFrom(csvSearchParams, includeFeeHeaders, includeMotoHeader);
            ObjectWriter writer = csvService.writerFrom(headers);
            Stopwatch stopwatch = Stopwatch.createStarted();
            outputStream.write(csvService.csvStringFrom(headers, writer).getBytes());
            do {
                page = transactionService.searchTransactionAfter(csvSearchParams, startingAfterCreatedDate, startingAfterId);
                count += page.size();

                if (!page.isEmpty()) {
                    var lastEntity = page.get(page.size() - 1);
                    startingAfterCreatedDate = lastEntity.getCreatedDate();
                    startingAfterId = lastEntity.getId();

                    outputStream.write(
                            csvService.csvStringFrom(page, writer).getBytes()
                    );
                    outputStream.flush();
                }
            } while (!page.isEmpty());
            outputStream.close();
            long elapsed = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
            LOGGER.info("CSV stream took:",
                    kv("time_taken_in_milli_seconds", elapsed),
                    kv("number_of_transactions_streamed", count));
        };
        return Response.ok(stream).build();
    }

    private TransactionSearchResponse searchForTransactions(TransactionSearchParams searchParams, Boolean overrideAccountRestriction, CommaDelimitedSetParameter commaSeparatedGatewayAccountIds, UriInfo uriInfo) {
        TransactionSearchParams transactionSearchParams = Optional.ofNullable(searchParams)
                .orElse(new TransactionSearchParams());
        validateSearchParams(transactionSearchParams, commaSeparatedGatewayAccountIds);
        List<String> gatewayAccountIds = commaSeparatedGatewayAccountIds != null ? commaSeparatedGatewayAccountIds.getParameters() : List.of();
        AccountIdListSupplierManager<TransactionSearchResponse> accountIdSupplierManager =
                AccountIdListSupplierManager.of(overrideAccountRestriction, gatewayAccountIds);
        return accountIdSupplierManager
                .withSupplier(accountId -> transactionService.searchTransactions(gatewayAccountIds, transactionSearchParams, uriInfo))
                .withPrivilegedSupplier(() -> transactionService.searchTransactions(transactionSearchParams, uriInfo))
                .validateAndGet(ACCOUNT_MANAGER_FIELD_NAME);
    }

    @Path("{transactionExternalId}/event")
    @GET
    @Timed
    @Operation(
            summary = "Get events for transaction external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = TransactionEventResponse.class))),
                    @ApiResponse(responseCode = "422", description = "Missing required query parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public TransactionEventResponse events(@Parameter(example = "9np5pocnotgkpp029d5kdfau5f", description = "Transaction external ID")
                                           @PathParam("transactionExternalId") String transactionExternalId,
                                           @Parameter(example = "1") @QueryParam("gateway_account_id") @NotEmpty String gatewayAccountId,
                                           @Parameter(description = "Set to 'true' to return all events. By default events that do not map to an external state are removed" +
                                                   " and duplicate events mapping to same external state are reduced to one event.", schema = @Schema(defaultValue = "false"))
                                           @QueryParam("include_all_events") boolean includeAllEvents,
                                           @Parameter(description = "Set to '2' to return failed transaction states FAILED_REJECTED/FAILED_EXPIRED/FAILED_CANCELLED" +
                                                   " mapped to declined/timedout/cancelled status respectively." +
                                                   "Otherwise these transaction states will all be mapped to `failed` status", schema = @Schema(defaultValue = "2"))
                                           @DefaultValue("2") @QueryParam("status_version") int statusVersion,
                                           @Context UriInfo uriInfo) {

        LOGGER.info("Get transaction event: external_id [{}], gateway_account_id [{}]",
                transactionExternalId, gatewayAccountId);
        return transactionService.findTransactionEvents(transactionExternalId, gatewayAccountId, includeAllEvents, statusVersion);
    }

    @Path("/{parentTransactionExternalId}/transaction")
    @GET
    @Timed
    @Operation(
            summary = "Get transactions (ex: refunds) related to parent transaction (payment)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = TransactionsForTransactionResponse.class))),
                    @ApiResponse(responseCode = "422", description = "Missing required query parameter (gateway_account_id)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public TransactionsForTransactionResponse getTransactionsForParentTransaction(
            @Parameter(example = "d0sk01d9amdk3ks0dk2dj03kd", description = "Parent transaction external ID", required = true)
            @PathParam("parentTransactionExternalId") String parentTransactionExternalId,
            @Parameter(example = "1", description = "Gateway account ID")
            @QueryParam("gateway_account_id") @NotEmpty String gatewayAccountId
    ) {
        LOGGER.info("Get transactions for parent transaction: [{}], gateway_account_id [{}]",
                parentTransactionExternalId, gatewayAccountId);

        return transactionService.getTransactions(parentTransactionExternalId, gatewayAccountId);
    }

    @Path("/gateway-transaction/{gatewayTransactionId}")
    @GET
    @Timed
    @Operation(
            summary = "Get transaction for a gateway transaction ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = TransactionView.class))),
                    @ApiResponse(responseCode = "422", description = "If payment_provider query parameter is missing", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public TransactionView findByGatewayTransactionId(@Parameter(example = "a14f0926-b44d-4160-8184-1b1f66e576ab", description = "Transaction ID from payment provider")
                                                      @PathParam("gatewayTransactionId") String gatewayTransactionId,
                                                      @Parameter(example = "sandbox", required = true) @QueryParam("payment_provider") @NotEmpty String paymentProvider
    ) {
        return transactionService.findByGatewayTransactionId(gatewayTransactionId, paymentProvider)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }
}