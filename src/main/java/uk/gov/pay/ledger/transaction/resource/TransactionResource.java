package uk.gov.pay.ledger.transaction.resource;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.transaction.service.AccountIdSupplierManager;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

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
                                   @QueryParam("account_id_is_not_required") Boolean overrideAccountRestriction,
                                   @Context UriInfo uriInfo) {
        LOGGER.info("Get transaction request: {}", transactionExternalId);

        return AccountIdSupplierManager.of(overrideAccountRestriction, gatewayAccountId)
                .withSupplier((accountId) -> transactionService.getTransactionForGatewayAccount(accountId, transactionExternalId, uriInfo))
                .withPrivilegedSupplier(() -> transactionService.getTransaction(transactionExternalId, uriInfo))
                .validateAndGet()
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    @Path("/")
    @GET
    @Timed
    public TransactionSearchResponse search(@Valid @BeanParam TransactionSearchParams searchParams,
                                            @Context UriInfo uriInfo) {

        if (searchParams == null) {
            searchParams = new TransactionSearchParams();
        }
        validateSearchParams(searchParams);
        return transactionService.searchTransactions(searchParams, uriInfo);
    }
}
