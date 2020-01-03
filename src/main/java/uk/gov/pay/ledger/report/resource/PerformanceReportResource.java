package uk.gov.pay.ledger.report.resource;

import com.codahale.metrics.annotation.Timed;
import uk.gov.pay.ledger.exception.ValidationException;
import uk.gov.pay.ledger.report.dao.PerformanceReportDao;
import uk.gov.pay.ledger.report.entity.PerformanceReportEntity;
import uk.gov.pay.ledger.report.params.PerformanceReportParams.PerformanceReportParamsBuilder;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.time.ZonedDateTime;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Path("/v1/report")
@Produces(APPLICATION_JSON)
public class PerformanceReportResource {

    private final PerformanceReportDao performanceReportDao;

    @Inject
    public PerformanceReportResource(PerformanceReportDao performanceReportDao) {
        this.performanceReportDao = performanceReportDao;
    }

    @Path("/performance-report")
    @GET
    @Timed
    public PerformanceReportEntity getPerformanceReport(@QueryParam("from_date") String fromDate,
                                                        @QueryParam("to_date") String toDate) {

        boolean dateParamsProvided = isNotBlank(fromDate) || isNotBlank(toDate);
        if (dateParamsProvided) {
            if (isBlank(fromDate) || isBlank(toDate)) {
                throw new ValidationException("Both from_date and to_date must be provided");
            } else {
                var params = PerformanceReportParamsBuilder.builder()
                        .withFromDate(ZonedDateTime.parse(fromDate))
                        .withToDate(ZonedDateTime.parse(toDate))
                        .build();
                return performanceReportDao.performanceReportForPaymentTransactions(params);
            }
        }

        return performanceReportDao.performanceReportForPaymentTransactions(PerformanceReportParamsBuilder.builder().build());
    }
}
