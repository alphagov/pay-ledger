package uk.gov.pay.ledger.report.params;

import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class PaymentsReportParamsTest {

    private PaymentsReportParams paymentsReportParams = new PaymentsReportParams();

    @Test
    public void getsEmptyFilterTemplateWhenEmptyFromDate() {
        paymentsReportParams.setFromDate("");
        assertThat(paymentsReportParams.getFilterTemplates().size(), is(0));
    }

    @Test
    public void getsFilterTemplateWhenValidFromDate() {
        paymentsReportParams.setFromDate("2018-09-22T10:14:16.067Z");
        assertThat(paymentsReportParams.getFilterTemplates().get(0), is(" t.created_date > :from_date"));
    }

    @Test
    public void getsEmptyFilterTemplateWhenEmptyToDate() {
        paymentsReportParams.setToDate("");
        assertThat(paymentsReportParams.getFilterTemplates().size(), is(0));
    }

    @Test
    public void getsFilterTemplateWhenValidToDate() {
        paymentsReportParams.setToDate("2018-09-22T10:14:16.067Z");
        assertThat(paymentsReportParams.getFilterTemplates().get(0), is(" t.created_date < :to_date"));
    }

    @Test
    public void getsEmptyFilterTemplateWhenEmptyAccountId() {
        paymentsReportParams.setAccountId("");
        assertThat(paymentsReportParams.getFilterTemplates().size(), is(0));
    }

    @Test
    public void getFilterTemplateWhenValidGatewayAccountId() {
        paymentsReportParams.setAccountId("1");
        assertThat(paymentsReportParams.getFilterTemplates().get(0), is(" t.gateway_account_id = :account_id"));
    }

    @Test
    public void getFilterTemplatesWhenAllFiltersSet() {
        paymentsReportParams.setAccountId("1");
        paymentsReportParams.setFromDate("2018-09-22T10:14:16.067Z");
        paymentsReportParams.setToDate("2018-09-22T10:14:16.067Z");
        assertThat(paymentsReportParams.getFilterTemplates(), hasSize(3));
    }

    @Test
    public void getsEmptyQueryMapWhenEmptyFromDate() {
        paymentsReportParams.setFromDate("");
        assertThat(paymentsReportParams.getQueryMap().size(), is(0));
    }

    @Test
    public void getsQueryMapWhenValidFromDate() {
        paymentsReportParams.setFromDate("2018-09-22T10:14:16.067Z");
        Map<String, Object> queryMap = paymentsReportParams.getQueryMap();
        assertThat(queryMap.size(), is(1));
        assertThat(queryMap.get("from_date").toString(), is("2018-09-22T10:14:16.067Z"));
    }

    @Test
    public void getsEmptyQueryMapWhenEmptyToDate() {
        paymentsReportParams.setToDate("");
        assertThat(paymentsReportParams.getQueryMap().size(), is(0));
    }

    @Test
    public void getsQueryMapWhenValidToDate() {
        paymentsReportParams.setToDate("2018-09-22T10:14:16.067Z");
        Map<String, Object> queryMap = paymentsReportParams.getQueryMap();
        assertThat(queryMap.size(), is(1));
        assertThat(queryMap.get("to_date").toString(), is("2018-09-22T10:14:16.067Z"));
    }

    @Test
    public void getsEmptyQueryMapWhenEmptyAccountId() {
            paymentsReportParams.setAccountId("");
        assertThat(paymentsReportParams.getQueryMap().size(), is(0));
    }

    @Test
    public void getsQueryMapWhenValidAccountId() {
        paymentsReportParams.setAccountId("1");
        Map<String, Object> queryMap = paymentsReportParams.getQueryMap();
        assertThat(queryMap.size(), is(1));
        assertThat(queryMap.get("account_id").toString(), is("1"));
    }

    @Test
    public void getsQueryMapWhenAllFiltersSet() {
        paymentsReportParams.setAccountId("1");
        paymentsReportParams.setFromDate("2018-09-22T10:14:16.067Z");
        paymentsReportParams.setToDate("2018-09-22T10:14:16.067Z");
        assertThat(paymentsReportParams.getQueryMap().size(), is(3));
    }
}