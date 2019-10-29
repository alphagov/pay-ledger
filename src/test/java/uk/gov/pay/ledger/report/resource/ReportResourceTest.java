package uk.gov.pay.ledger.report.resource;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.exception.BadRequestExceptionMapper;
import uk.gov.pay.ledger.exception.JerseyViolationExceptionMapper;
import uk.gov.pay.ledger.report.entity.TransactionsStatisticsResult;
import uk.gov.pay.ledger.report.entity.TransactionSummaryResult;
import uk.gov.pay.ledger.report.params.TransactionSummaryParams;
import uk.gov.pay.ledger.report.service.ReportService;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportResourceTest {

    private static final ReportService mockReportService = mock(ReportService.class);

    @ClassRule
    public static ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new ReportResource(mockReportService))
            .addProvider(BadRequestExceptionMapper.class)
            .addProvider(JerseyViolationExceptionMapper.class)
            .build();

    @Test
    public void getPaymentsByState_shouldReturn400IfGatewayAccountIsNotProvided() {
        Response response = resources
                .target("/v1/report/payments_by_state")
                .request()
                .get();

        assertThat(response.getStatus(), is(400));

        Map responseMap = response.readEntity(Map.class);
        assertThat(responseMap.get("message"), is(List.of("Field [account_id] cannot be empty")));
    }

    @Test
    public void getPaymentsByState_shouldReturn200IfGatewayAccountIdIsNotProvidedButNotRequiredFlag() {
        when(mockReportService.getPaymentCountsByState(new TransactionSummaryParams()))
                .thenReturn(Map.of("blah", 1L));

        Response response = resources
                .target("/v1/report/payments_by_state")
                .queryParam("override_account_id_restriction", true)
                .request()
                .get();

        Assert.assertThat(response.getStatus(), CoreMatchers.is(200));
    }

    @Test
    public void getPaymentsByState_shouldReturn422IfFromDateInvalid() {
        Response response = resources
                .target("/v1/report/payments_by_state")
                .queryParam("account_id", "abc123")
                .queryParam("from_date", "invalid")
                .request()
                .get();

        assertThat(response.getStatus(), is(422));

        Map responseMap = response.readEntity(Map.class);
        assertThat(responseMap.get("message"), is(List.of("Invalid attribute value: from_date. Must be a valid date")));
    }

    @Test
    public void getPaymentsByState_shouldReturn422IfToDateInvalid() {
        Response response = resources
                .target("/v1/report/payments_by_state")
                .queryParam("account_id", "abc123")
                .queryParam("to_date", "invalid")
                .request()
                .get();

        assertThat(response.getStatus(), is(422));

        Map responseMap = response.readEntity(Map.class);
        assertThat(responseMap.get("message"), is(List.of("Invalid attribute value: to_date. Must be a valid date")));
    }

    @Test
    public void getPaymentsStatistics_shouldReturn400IfGatewayAccountIsNotProvided() {
        Response response = resources
                .target("/v1/report/transactions-summary")
                .queryParam("override_from_date_validation", true)
                .request()
                .get();

        assertThat(response.getStatus(), is(400));

        Map responseMap = response.readEntity(Map.class);
        assertThat(responseMap.get("message"), is(List.of("Field [account_id] cannot be empty")));
    }

    @Test
    public void getTransactionSummary_shouldReturn200IfGatewayAccountIdIsNotProvidedButNotRequiredFlag() {
        when(mockReportService.getTransactionsSummary(new TransactionSummaryParams()))
                .thenReturn(new TransactionSummaryResult(new TransactionsStatisticsResult(200L, 20000L), new TransactionsStatisticsResult(0L, 0L)));

        Response response = resources
                .target("/v1/report/transactions-summary")
                .queryParam("override_account_id_restriction", true)
                .queryParam("override_from_date_validation", true)
                .request()
                .get();

        Assert.assertThat(response.getStatus(), CoreMatchers.is(200));
    }

    @Test
    public void getTransactionSummary_shouldReturn422IfFromDateInvalid() {
        Response response = resources
                .target("/v1/report/transactions-summary")
                .queryParam("account_id", "abc123")
                .queryParam("from_date", "invalid")
                .request()
                .get();

        assertThat(response.getStatus(), is(422));

        Map responseMap = response.readEntity(Map.class);
        assertThat(responseMap.get("message"), is(List.of("Invalid attribute value: from_date. Must be a valid date")));
    }

    @Test
    public void getTransactionSummary_shouldReturn422IfToDateInvalid() {
        Response response = resources
                .target("/v1/report/transactions-summary")
                .queryParam("account_id", "abc123")
                .queryParam("to_date", "invalid")
                .queryParam("override_from_date_validation", true)
                .request()
                .get();

        assertThat(response.getStatus(), is(422));

        Map responseMap = response.readEntity(Map.class);
        assertThat(responseMap.get("message"), is(List.of("Invalid attribute value: to_date. Must be a valid date")));
    }

    @Test
    public void getTransactionSummary_shouldReturn400IfGatewayAccountIsNotProvided() {
        Response response = resources
                .target("/v1/report/transactions-summary")
                .queryParam("from_date", "2019-10-01T09:00:00.000Z")
                .request()
                .get();

        assertThat(response.getStatus(), is(400));

        Map responseMap = response.readEntity(Map.class);
        assertThat(responseMap.get("message"), is(List.of("Field [account_id] cannot be empty")));
    }

    @Test
    public void getTransactionSummary_shouldReturn400IfNoFromDateAndValidationIsNotOverriden() {
        Response response = resources
                .target("/v1/report/transactions-summary")
                .queryParam("account_id", "abc123")
                .request()
                .get();

        assertThat(response.getStatus(), is(400));

        Map responseMap = response.readEntity(Map.class);
        assertThat(responseMap.get("message"), is(List.of("Field [from_date] can not be null")));
    }
}