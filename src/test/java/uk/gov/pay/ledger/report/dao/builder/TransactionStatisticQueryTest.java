package uk.gov.pay.ledger.report.dao.builder;


import org.junit.Before;
import org.junit.Test;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class TransactionStatisticQueryTest {

    private TransactionStatisticQuery transactionStatisticQuery = new TransactionStatisticQuery();

    @Before
    public void setup() {
        transactionStatisticQuery.getQueryMap().clear();
        transactionStatisticQuery.getFilterTemplates().clear();
    }

    @Test
    public void getsFilterTemplateWhenValidFromDate() {
        transactionStatisticQuery.withFromDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionStatisticQuery.getFilterTemplates().get(0), is(" t.created_date > :from_date"));
    }

    @Test
    public void getsFilterTemplateWhenValidToDate() {
        transactionStatisticQuery.withToDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionStatisticQuery.getFilterTemplates().get(0), is(" t.created_date < :to_date"));
    }

    @Test
    public void getFilterTemplateWhenValidGatewayAccountId() {
        transactionStatisticQuery.withAccountId("1");
        assertThat(transactionStatisticQuery.getFilterTemplates().get(0), is(" t.gateway_account_id = :account_id"));
    }

    @Test
    public void getFilterTemplateWhenValidMotoFlag() {
        transactionStatisticQuery.withMoto(true);
        assertThat(transactionStatisticQuery.getFilterTemplates().get(0), is(" t.moto = :moto"));
    }

    @Test
    public void getFilterTemplatesWhenAllFiltersSet() {
        transactionStatisticQuery.withAccountId("1");
        transactionStatisticQuery.withMoto(true);
        transactionStatisticQuery.withFromDate("2018-09-22T10:14:16.067Z");
        transactionStatisticQuery.withToDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionStatisticQuery.getFilterTemplates(), hasSize(4));
    }


    @Test
    public void getsQueryMapWhenValidFromDate() {
        transactionStatisticQuery.withFromDate("2018-09-22T10:14:16.067Z");
        Map<String, Object> queryMap = transactionStatisticQuery.getQueryMap();
        assertThat(queryMap.size(), is(1));
        assertThat(queryMap.get("from_date").toString(), is("2018-09-22T10:14:16.067Z"));
    }

    @Test
    public void getsQueryMapWhenValidToDate() {
        transactionStatisticQuery.withToDate("2018-09-22T10:14:16.067Z");
        Map<String, Object> queryMap = transactionStatisticQuery.getQueryMap();
        assertThat(queryMap.size(), is(1));
        assertThat(queryMap.get("to_date").toString(), is("2018-09-22T10:14:16.067Z"));
    }

    @Test
    public void getsQueryMapWhenValidAccountId() {
        transactionStatisticQuery.withAccountId("1");
        Map<String, Object> queryMap = transactionStatisticQuery.getQueryMap();
        assertThat(queryMap.size(), is(1));
        assertThat(queryMap.get("account_id").toString(), is("1"));
    }

    @Test
    public void getsQueryMapWhenAllFiltersSet() {
        transactionStatisticQuery.withAccountId("1");
        transactionStatisticQuery.withFromDate("2018-09-22T10:14:16.067Z");
        transactionStatisticQuery.withToDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionStatisticQuery.getQueryMap().size(), is(3));
    }
}