package uk.gov.pay.ledger.report.resource;

import com.codahale.metrics.annotation.Timed;
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
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Path("/v1/report")
@Produces(APPLICATION_JSON)
public class PerformanceReportResource {

    private final PerformanceReportDao performanceReportDao;
    private final TransactionSummaryDao transactionSummaryDao;

    @Inject
    public PerformanceReportResource(PerformanceReportDao performanceReportDao, TransactionSummaryDao transactionSummaryDao) {
        this.performanceReportDao = performanceReportDao;
        this.transactionSummaryDao = transactionSummaryDao;
    }

    @Path("/performance-report")
    @GET
    @Timed
    public PerformanceReportEntity getPerformanceReport(@QueryParam("from_date") String fromDate,
                                                        @QueryParam("to_date") String toDate,
                                                        @QueryParam("state") String state) {

        var paramsBuilder = PerformanceReportParamsBuilder.builder();
        addDateRangeParamsOrThrow(paramsBuilder, fromDate, toDate);
        addStateParamOrThrow(paramsBuilder, state);
        return performanceReportDao.performanceReportForPaymentTransactions(paramsBuilder.build());
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
                builder.withFromDate(ZonedDateTime.parse(fromDate)).withToDate(ZonedDateTime.parse(toDate));
            }
        }
    }

    @Path("/gateway-performance-report")
    @GET
    @Timed
    public List<GatewayAccountMonthlyPerformanceReportEntity> getGatewayMonthlyPerformanceReport(@QueryParam("from_date") String fromDate,
                                                                                                 @QueryParam("to_date") String toDate) {
        if (isBlank(fromDate) || isBlank(toDate)) {
            throw new ValidationException("Both from_date and to_date must be provided");
        } else if (LocalDate.parse(fromDate).isAfter(LocalDate.parse(toDate))) {
            throw new ValidationException("from_date must be earlier or equal to to_date");
        }

        return transactionSummaryDao.monthlyPerformanceReportForGatewayAccounts(LocalDate.parse(fromDate), LocalDate.parse(toDate));
    }
}
