package uk.gov.pay.ledger.report.params;

import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class TransactionSummaryParamsTest {

    private TransactionSummaryParams transactionSummaryParams = new TransactionSummaryParams();

    @Test
    public void getsEmptyFilterTemplateWhenEmptyFromDate() {
        transactionSummaryParams.setFromDate("");
        assertThat(transactionSummaryParams.getFilterTemplates().size(), is(0));
    }

    @Test
    public void getsFilterTemplateWhenValidFromDate() {
        transactionSummaryParams.setFromDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionSummaryParams.getFilterTemplates().get(0), is(" t.created_date > :from_date"));
    }

    @Test
    public void getsEmptyFilterTemplateWhenEmptyToDate() {
        transactionSummaryParams.setToDate("");
        assertThat(transactionSummaryParams.getFilterTemplates().size(), is(0));
    }

    @Test
    public void getsFilterTemplateWhenValidToDate() {
        transactionSummaryParams.setToDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionSummaryParams.getFilterTemplates().get(0), is(" t.created_date < :to_date"));
    }

    @Test
    public void getsEmptyFilterTemplateWhenEmptyAccountId() {
        transactionSummaryParams.setAccountId("");
        assertThat(transactionSummaryParams.getFilterTemplates().size(), is(0));
    }

    @Test
    public void getFilterTemplateWhenValidGatewayAccountId() {
        transactionSummaryParams.setAccountId("1");
        assertThat(transactionSummaryParams.getFilterTemplates().get(0), is(" t.gateway_account_id = :account_id"));
    }

    @Test
    public void getFilterTemplatesWhenAllFiltersSet() {
        transactionSummaryParams.setAccountId("1");
        transactionSummaryParams.setFromDate("2018-09-22T10:14:16.067Z");
        transactionSummaryParams.setToDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionSummaryParams.getFilterTemplates(), hasSize(3));
    }

    @Test
    public void getsEmptyQueryMapWhenEmptyFromDate() {
        transactionSummaryParams.setFromDate("");
        assertThat(transactionSummaryParams.getQueryMap().size(), is(0));
    }

    @Test
    public void getsQueryMapWhenValidFromDate() {
        transactionSummaryParams.setFromDate("2018-09-22T10:14:16.067Z");
        Map<String, Object> queryMap = transactionSummaryParams.getQueryMap();
        assertThat(queryMap.size(), is(1));
        assertThat(queryMap.get("from_date").toString(), is("2018-09-22T10:14:16.067Z"));
    }

    @Test
    public void getsEmptyQueryMapWhenEmptyToDate() {
        transactionSummaryParams.setToDate("");
        assertThat(transactionSummaryParams.getQueryMap().size(), is(0));
    }

    @Test
    public void getsQueryMapWhenValidToDate() {
        transactionSummaryParams.setToDate("2018-09-22T10:14:16.067Z");
        Map<String, Object> queryMap = transactionSummaryParams.getQueryMap();
        assertThat(queryMap.size(), is(1));
        assertThat(queryMap.get("to_date").toString(), is("2018-09-22T10:14:16.067Z"));
    }

    @Test
    public void getsEmptyQueryMapWhenEmptyAccountId() {
            transactionSummaryParams.setAccountId("");
        assertThat(transactionSummaryParams.getQueryMap().size(), is(0));
    }

    @Test
    public void getsQueryMapWhenValidAccountId() {
        transactionSummaryParams.setAccountId("1");
        Map<String, Object> queryMap = transactionSummaryParams.getQueryMap();
        assertThat(queryMap.size(), is(1));
        assertThat(queryMap.get("account_id").toString(), is("1"));
    }

    @Test
    public void getsQueryMapWhenAllFiltersSet() {
        transactionSummaryParams.setAccountId("1");
        transactionSummaryParams.setFromDate("2018-09-22T10:14:16.067Z");
        transactionSummaryParams.setToDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionSummaryParams.getQueryMap().size(), is(3));
    }
}