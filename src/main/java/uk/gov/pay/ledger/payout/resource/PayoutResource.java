package uk.gov.pay.ledger.payout.resource;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payouts")
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
    @Operation(
            summary = "Search payouts by gateway_account_id and state",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PayoutSearchResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public PayoutSearchResponse search(@BeanParam PayoutSearchParams searchParams,
                                       @QueryParam("override_account_id_restriction") @Parameter(description = "Set to true to list all payouts.")
                                               Boolean overrideAccountRestriction,
                                       @QueryParam("gateway_account_id") @Parameter(description = "Comma separate gateway account IDs. Required except when override_account_id_restriction=true", required = true, example = "1,2", schema = @Schema(type = "string", implementation = String.class))
                                               CommaDelimitedSetParameter gatewayAccountIds,
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
