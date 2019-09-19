package uk.gov.pay.ledger.transaction.search.common;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class TransactionSearchParamsTest {

    private TransactionSearchParams transactionSearchParams;

    @Before
    public void setUp() {
        transactionSearchParams = new TransactionSearchParams();
    }

    @Test
    public void getsEmptyFilterAndQueryMapWhenEmptyReference() {
        transactionSearchParams.setReference("");
        assertThat(transactionSearchParams.getFilterTemplates().size(), is(0));
        assertThat(transactionSearchParams.getQueryMap().size(), is(0));
    }

    @Test
    public void getsFilterAndQueryMapWhenNotEmptyReference() {
        transactionSearchParams.setReference("test-reference");
        assertThat(transactionSearchParams.getFilterTemplates().get(0), is(" lower(t.reference) ILIKE :reference"));
        assertThat(transactionSearchParams.getQueryMap().get("reference"), is("%test-reference%"));
    }

    @Test
    public void getsFilterAndQueryMapWhenNotEmptyReferenceAndExactMatch() {
        transactionSearchParams.setReference("test-reference");
        transactionSearchParams.setExactReferenceMatch(true);
        assertThat(transactionSearchParams.getFilterTemplates().get(0), is(" lower(t.reference) = lower(:reference)"));
        assertThat(transactionSearchParams.getQueryMap().get("reference"), is("test-reference"));
    }

    @Test
    public void getsEmptyFilterTemplateWhenEmptyFromDate() {
        transactionSearchParams.setFromDate("");
        assertThat(transactionSearchParams.getFilterTemplates().size(), is(0));
    }

    @Test
    public void getsFilterTemplateWhenValidFromDate() {
        transactionSearchParams.setFromDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionSearchParams.getFilterTemplates().get(0), is(" t.created_date > :from_date"));
    }

    @Test
    public void getsEmptyFilterTemplateWhenEmptyToDate() {
        transactionSearchParams.setToDate("");
        assertThat(transactionSearchParams.getFilterTemplates().size(), is(0));
    }

    @Test
    public void getsFilterTemplateWhenValidToDate() {
        transactionSearchParams.setToDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionSearchParams.getFilterTemplates().get(0), is(" t.created_date < :to_date"));
    }

    @Test
    public void getsEmptyQueryMapWhenEmptyCardHolderName() {
        transactionSearchParams.setCardHolderName("");
        assertThat(transactionSearchParams.getQueryMap().size(), is(0));
    }

    @Test
    public void getsQueryMapWhenNotEmptyCardHolderName() {
        transactionSearchParams.setCardHolderName("Jan Kowalski");
        assertThat(transactionSearchParams.getQueryMap().size(), is(1));
        assertThat(transactionSearchParams.getQueryMap().get("cardholder_name"), is("%Jan Kowalski%"));
    }

    @Test
    public void getsEmptyQueryMapWhenEmptyFromDate() {
        transactionSearchParams.setFromDate("");
        assertThat(transactionSearchParams.getQueryMap().size(), is(0));
    }

    @Test
    public void getsQueryMapWhenValidFromDate() {
        transactionSearchParams.setFromDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionSearchParams.getQueryMap().size(), is(1));
        assertThat(transactionSearchParams.getQueryMap().get("from_date").toString(), is("2018-09-22T10:14:16.067Z"));
    }

    @Test
    public void getsEmptyQueryMapWhenEmptyToDate() {
        transactionSearchParams.setToDate("");
        assertThat(transactionSearchParams.getQueryMap().size(), is(0));
    }

    @Test
    public void getsQueryMapWhenValidToDate() {
        transactionSearchParams.setToDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionSearchParams.getQueryMap().size(), is(1));
        assertThat(transactionSearchParams.getQueryMap().get("to_date").toString(), is("2018-09-22T10:14:16.067Z"));
    }

    @Test
    public void getsEmptyQueryParamStringWhenEmptyAccountId() {
        transactionSearchParams.setAccountId("");
        assertThat(transactionSearchParams.buildQueryParamString(1L), not(containsString("account_id")));
    }

    @Test
    public void getsQueryParamStringWhenNotEmptyAccountId() {
        transactionSearchParams.setAccountId("xyz");
        assertThat(transactionSearchParams.buildQueryParamString(1L), containsString("account_id=xyz"));
    }

    @Test
    public void getsEmptyQueryParamStringWhenEmptyFromDate() {
        transactionSearchParams.setFromDate("");
        assertThat(transactionSearchParams.buildQueryParamString(1L), not(containsString("from_date")));
    }

    @Test
    public void getsQueryParamStringWhenNotEmptyFromDate() {
        transactionSearchParams.setFromDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionSearchParams.buildQueryParamString(1L), containsString("from_date=2018-09-22T10:14:16.067Z"));
    }

    @Test
    public void getsEmptyQueryParamStringWhenEmptyToDate() {
        transactionSearchParams.setToDate("");
        assertThat(transactionSearchParams.buildQueryParamString(1L), not(containsString("to_date")));
    }

    @Test
    public void getsQueryParamStringWhenNotEmptyToDate() {
        transactionSearchParams.setToDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionSearchParams.buildQueryParamString(1L), containsString("to_date=2018-09-22T10:14:16.067Z"));
    }
}