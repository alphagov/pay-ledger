package uk.gov.pay.ledger.transaction.resource;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.transaction.model.TransactionEventResponse;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.model.TransactionsForTransactionResponse;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.transaction.service.AccountIdSupplierManager;
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
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static uk.gov.pay.ledger.transaction.search.common.TransactionSearchParamsValidator.validateSearchParams;

@Path("/v1/transaction")
@Produces(APPLICATION_JSON)
public class TransactionResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionResource.class);
    private final TransactionService transactionService;

    @Inject
    public TransactionResource(TransactionService transactionService) {
        this.transactionService = transactionService;
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
    public List<Map<String,Object>> searchCsv(@Valid
                                            @BeanParam TransactionSearchParams searchParams,
                                              @QueryParam("override_account_id_restriction") Boolean overrideAccountRestriction,
                                              @QueryParam("account_id") String gatewayAccountId,
                                              @Context UriInfo uriInfo) {
        TransactionSearchParams csvSearchParams = Optional.ofNullable(searchParams)
                .orElse(new TransactionSearchParams());
        csvSearchParams.overrideMaxDisplaySize(100000L);
        csvSearchParams.setAccountId(gatewayAccountId);
        csvSearchParams.setWithParentTransaction(true);

        validateSearchParams(csvSearchParams, gatewayAccountId);

        return transactionService.searchTransactionsForCsv(searchParams);
    }

    private TransactionSearchResponse searchForTransactions(TransactionSearchParams searchParams, Boolean overrideAccountRestriction, String gatewayAccountId, UriInfo uriInfo) {
        TransactionSearchParams transactionSearchParams = Optional.ofNullable(searchParams)
                .orElse(new TransactionSearchParams());

        validateSearchParams(transactionSearchParams, gatewayAccountId);
        AccountIdSupplierManager<TransactionSearchResponse> accountIdSupplierManager =
                AccountIdSupplierManager.of(overrideAccountRestriction, gatewayAccountId);

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
