package uk.gov.pay.ledger.payout.resource;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.payout.model.PayoutSearchResponse;
import uk.gov.pay.ledger.payout.search.PayoutSearchParams;
import uk.gov.pay.ledger.payout.service.PayoutService;
import uk.gov.pay.ledger.transaction.service.AccountIdListSupplierManager;
import uk.gov.pay.ledger.util.CommaDelimitedSetParameter;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/payout")
@Produces(APPLICATION_JSON)
public class PayoutResource {

    private static final String ACCOUNT_MANAGER_FIELD_NAME = "gateway_account_id";
    private PayoutService payoutService;

    @Inject
    public PayoutResource(PayoutService payoutService) {
        this.payoutService = payoutService;
    }

    @Path("/")
    @GET
    @Timed
    public PayoutSearchResponse search(@BeanParam PayoutSearchParams searchParams,
                                       @QueryParam("override_account_id_restriction") Boolean overrideAccountRestriction,
                                       @QueryParam("gateway_account_id") CommaDelimitedSetParameter gatewayAccountIds,
                                       @Context UriInfo uriInfo) {

        return searchForPayouts(searchParams, overrideAccountRestriction, gatewayAccountIds, uriInfo);
    }

    private PayoutSearchResponse searchForPayouts(PayoutSearchParams searchParams,
                                                  Boolean overrideAccountRestriction,
                                                  CommaDelimitedSetParameter commaSeparatedGatewayAccountIds,
                                                  UriInfo uriInfo) {
        PayoutSearchParams transactionSearchParams = Optional.ofNullable(searchParams)
                .orElse(new PayoutSearchParams());
        List<String> gatewayAccountIds = commaSeparatedGatewayAccountIds != null ? commaSeparatedGatewayAccountIds.getParameters() : List.of();
        AccountIdListSupplierManager<PayoutSearchResponse> accountIdSupplierManager =
                AccountIdListSupplierManager.of(overrideAccountRestriction, gatewayAccountIds);

        return accountIdSupplierManager
                .withSupplier(accountId -> payoutService.searchPayouts(gatewayAccountIds, transactionSearchParams, uriInfo))
                .withPrivilegedSupplier(() -> payoutService.searchPayouts(transactionSearchParams, uriInfo))
                .validateAndGet(ACCOUNT_MANAGER_FIELD_NAME);
    }
}
