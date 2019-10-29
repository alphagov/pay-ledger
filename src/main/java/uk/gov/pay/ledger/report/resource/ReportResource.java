package uk.gov.pay.ledger.report.resource;

import com.codahale.metrics.annotation.Timed;
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
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/report")
@Produces(APPLICATION_JSON)
public class ReportResource {

    private final ReportService reportService;

    @Inject
    public ReportResource(ReportService reportService) {
        this.reportService = reportService;
    }

    @Path("/payments_by_state")
    @GET
    @Timed
    public Response getPaymentCountsByState(
            @Valid @BeanParam TransactionSummaryParams transactionSummaryParams,
            @QueryParam("override_account_id_restriction") Boolean overrideAccountRestriction,
            @QueryParam("account_id") String gatewayAccountId) {

        AccountIdSupplierManager<Map<String, Long>> accountIdSupplierManager =
                AccountIdSupplierManager.of(overrideAccountRestriction, gatewayAccountId);

        Map<String, Long> paymentCountsByState = accountIdSupplierManager
                .withSupplier(accountId -> reportService.getPaymentCountsByState(transactionSummaryParams))
                .withPrivilegedSupplier(() -> reportService.getPaymentCountsByState(transactionSummaryParams))
                .validateAndGet();

        return Response.status(OK).entity(paymentCountsByState).build();
    }

    @Path("/transactions-summary")
    @GET
    @Timed
    public TransactionSummaryResult getTransactionSummaryResult(
            @Valid @BeanParam TransactionSummaryParams transactionSummaryParams,
            @QueryParam("override_account_id_restriction") Boolean overrideAccountRestriction,
            @QueryParam("override_from_date_validation") @DefaultValue("false") Boolean overrideFromDateValidation) {

        TransactionSummaryValidator.validateTransactionSummaryParams(transactionSummaryParams, overrideFromDateValidation);

        AccountIdSupplierManager<TransactionSummaryResult> accountIdSupplierManager =
                AccountIdSupplierManager.of(overrideAccountRestriction, transactionSummaryParams.getAccountId());

        return accountIdSupplierManager
                .withSupplier(accountId -> reportService.getTransactionsSummary(transactionSummaryParams))
                .withPrivilegedSupplier(() -> reportService.getTransactionsSummary(transactionSummaryParams))
                .validateAndGet();
    }
}
