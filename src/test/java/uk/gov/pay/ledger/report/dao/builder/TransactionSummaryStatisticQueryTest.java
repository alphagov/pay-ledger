package uk.gov.pay.ledger.report.dao.builder;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class TransactionSummaryStatisticQueryTest {

    private TransactionSummaryStatisticQuery transactionSummaryStatisticQuery = new TransactionSummaryStatisticQuery();

    @BeforeEach
    public void setup() {
        transactionSummaryStatisticQuery.getQueryMap().clear();
        transactionSummaryStatisticQuery.getFilterTemplates().clear();
    }

    @Test
    public void getsFilterTemplateWhenValidFromDate() {
        transactionSummaryStatisticQuery.withFromDate("2018-09-22T10:00:00Z");
        assertThat(transactionSummaryStatisticQuery.getFilterTemplates().get(0), is(" t.transaction_date >= :from_date"));
    }

    @Test
    public void getsFilterTemplateWhenValidToDate() {
        transactionSummaryStatisticQuery.withToDate("2018-09-22T10:00:00Z");
        assertThat(transactionSummaryStatisticQuery.getFilterTemplates().get(0), is(" t.transaction_date <= :to_date"));
    }

    @Test
    public void getFilterTemplateWhenValidGatewayAccountId() {
        transactionSummaryStatisticQuery.withAccountId("1");
        assertThat(transactionSummaryStatisticQuery.getFilterTemplates().get(0), is(" t.gateway_account_id = :account_id"));
    }

    @Test
    public void getFilterTemplateWhenValidMotoFlag() {
        transactionSummaryStatisticQuery.withMoto(true);
        assertThat(transactionSummaryStatisticQuery.getFilterTemplates().get(0), is(" t.moto = :moto"));
    }

    @Test
    public void getFilterTemplatesWhenAllFiltersSet() {
        transactionSummaryStatisticQuery.withAccountId("1");
        transactionSummaryStatisticQuery.withMoto(true);
        transactionSummaryStatisticQuery.withFromDate("2018-09-22T10:00:00Z");
        transactionSummaryStatisticQuery.withToDate("2018-09-22T10:00:00Z");
        assertThat(transactionSummaryStatisticQuery.getFilterTemplates(), hasSize(4));
    }


    @Test
    public void getsQueryMapWhenValidFromDate() {
        transactionSummaryStatisticQuery.withFromDate("2018-09-22T10:00:00Z");
        Map<String, Object> queryMap = transactionSummaryStatisticQuery.getQueryMap();
        assertThat(queryMap.size(), is(1));
        assertThat(queryMap.get("from_date").toString(), is("2018-09-22"));
    }

    @Test
    public void getsQueryMapWhenValidToDate() {
        transactionSummaryStatisticQuery.withToDate("2018-09-22T10:00:00Z");
        Map<String, Object> queryMap = transactionSummaryStatisticQuery.getQueryMap();
        assertThat(queryMap.size(), is(1));
        assertThat(queryMap.get("to_date").toString(), is("2018-09-22"));
    }

    @Test
    public void getsQueryMapWhenValidAccountId() {
        transactionSummaryStatisticQuery.withAccountId("1");
        Map<String, Object> queryMap = transactionSummaryStatisticQuery.getQueryMap();
        assertThat(queryMap.size(), is(1));
        assertThat(queryMap.get("account_id").toString(), is("1"));
    }

    @Test
    public void getsQueryMapWhenAllFiltersSet() {
        transactionSummaryStatisticQuery.withAccountId("1");
        transactionSummaryStatisticQuery.withFromDate("2018-09-22T10:00:00Z");
        transactionSummaryStatisticQuery.withToDate("2018-09-22T10:00:00Z");
        assertThat(transactionSummaryStatisticQuery.getQueryMap().size(), is(3));
    }
}