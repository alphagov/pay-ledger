package uk.gov.pay.ledger.transaction.resource;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.inject.Inject;
import jersey.repackaged.com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.TransactionEventResponse;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.model.TransactionsForTransactionResponse;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.transaction.service.AccountIdSupplierManager;
import uk.gov.pay.ledger.transaction.service.CsvService;
import uk.gov.pay.ledger.transaction.service.TransactionService;

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
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.pay.ledger.transaction.search.common.TransactionSearchParamsValidator.validateSearchParams;

@Path("/v1/transaction")
@Produces(APPLICATION_JSON)
public class TransactionResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionResource.class);
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
    @Produces("application/json")
    @Timed
    public TransactionSearchResponse search(@Valid
                                            @BeanParam TransactionSearchParams searchParams,
                                            @QueryParam("override_account_id_restriction") Boolean overrideAccountRestriction,
                                            @QueryParam("account_id") String gatewayAccountId,
                                            @Context UriInfo uriInfo) {
        return searchForTransactions(searchParams, overrideAccountRestriction, gatewayAccountId, uriInfo);
    }

    @Path("/")
    @GET
    @Produces("text/csv")
    @Timed
    public Response streamCsv(@Valid @BeanParam TransactionSearchParams searchParams,
                              @QueryParam("override_account_id_restriction") Boolean overrideAccountRestriction,
                              @QueryParam("account_id") String gatewayAccountId,
                              @QueryParam("fee_headers") boolean includeFeeHeaders,
                              @Context UriInfo uriInfo) {
        StreamingOutput stream = outputStream -> {
            TransactionSearchParams csvSearchParams = Optional.ofNullable(searchParams).orElse(new TransactionSearchParams());
            LOGGER.error("debugging travis - reporting config",
                    kv("reporting config", configuration.getReportingConfig()));

            LOGGER.error("debugging travis - csv page size",
                    kv("streaming csv page size", configuration.getReportingConfig().getStreamingCsvPageSize()));

            csvSearchParams.overrideMaxDisplaySize((long) configuration.getReportingConfig().getStreamingCsvPageSize());
            csvSearchParams.setAccountId(gatewayAccountId);
            List<TransactionEntity> page;
            ZonedDateTime startingAfterCreatedDate = null;
            Long startingAfterId = null;
            int count = 0;

            Map<String, Object> headers = csvService.csvHeaderFrom(searchParams, includeFeeHeaders);
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
            } while (!page.isEmpty() && count < configuration.getReportingConfig().getMaximumCsvRowsSize());
            outputStream.close();
            long elapsed = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
            LOGGER.info("CSV stream took:",
                    kv("time_taken_in_milli_seconds", elapsed),
                    kv("number_of_transactions_streamed", count));
        };
        return Response.ok(stream).build();
    }

    private TransactionSearchResponse searchForTransactions(TransactionSearchParams searchParams, Boolean overrideAccountRestriction, String gatewayAccountId, UriInfo uriInfo) {
        TransactionSearchParams transactionSearchParams = Optional.ofNullable(searchParams)
                .orElse(new TransactionSearchParams());

        validateSearchParams(transactionSearchParams, gatewayAccountId);
        AccountIdSupplierManager<TransactionSearchResponse> accountIdSupplierManager =
                AccountIdSupplierManager.of(overrideAccountRestriction, gatewayAccountId);

        LOGGER.error("Should be validating the account id next",
                kv("account id", gatewayAccountId),
                kv("override account restriction",accountIdSupplierManager.overrideAccountRestriction));
        return accountIdSupplierManager
                .withSupplier(accountId -> transactionService.searchTransactions(gatewayAccountId, transactionSearchParams, uriInfo))
                .withPrivilegedSupplier(() -> transactionService.searchTransactions(transactionSearchParams, uriInfo))
                .validateAndGet();
    }

    @Path("{transactionExternalId}/event")
    @GET
    @Timed
    public TransactionEventResponse events(@PathParam("transactionExternalId") String transactionExternalId,
                                           @QueryParam("gateway_account_id") @NotEmpty String gatewayAccountId,
                                           @QueryParam("include_all_events") boolean includeAllEvents,
                                           @DefaultValue("2") @QueryParam("status_version") int statusVersion,
                                           @Context UriInfo uriInfo) {

        LOGGER.info("Get transaction event: external_id [{}], gateway_account_id [{}]",
                transactionExternalId, gatewayAccountId);
        return transactionService.findTransactionEvents(transactionExternalId, gatewayAccountId, includeAllEvents, statusVersion);
    }

    @Path("/{parentTransactionExternalId}/transaction")
    @GET
    @Timed
    public TransactionsForTransactionResponse getTransactionsForParentTransaction(@PathParam("parentTransactionExternalId") String parentTransactionExternalId,
                                                                                  @QueryParam("gateway_account_id") @NotEmpty String gatewayAccountId
    ) {
        LOGGER.info("Get transactions for parent transaction: [{}], gateway_account_id [{}]",
                parentTransactionExternalId, gatewayAccountId);

        return transactionService.getTransactions(parentTransactionExternalId, gatewayAccountId);
    }
}
