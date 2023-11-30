package uk.gov.pay.ledger.transaction.search.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransactionSearchParamsTest {

    private TransactionSearchParams transactionSearchParams;

    @BeforeEach
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
        assertThat(transactionSearchParams.getFilterTemplates().get(0), is(" lower(t.reference) LIKE lower(:reference)"));
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
    public void shouldApplyGatewayAccountIdCorrectlyToFilterAndQueries() {
        transactionSearchParams.setGatewayPayoutId("test-gateway-payout-id");
        assertThat(transactionSearchParams.getFilterTemplates().get(0), is(" t.gateway_payout_id = :gateway_payout_id"));
        assertThat(transactionSearchParams.getQueryMap().get("gateway_payout_id"), is("test-gateway-payout-id"));
        assertThat(transactionSearchParams.buildQueryParamString(1L), containsString("gateway_payout_id=test-gateway-payout-id"));
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
        transactionSearchParams.setAccountIds(List.of());
        assertThat(transactionSearchParams.buildQueryParamString(1L), not(containsString("account_id")));
    }

    @Test
    public void getsQueryParamStringWhenNotEmptyAccountId() {
        transactionSearchParams.setAccountIds(List.of("xyz"));
        assertThat(transactionSearchParams.buildQueryParamString(1L), containsString("account_id=xyz"));
    }

    @Test
    public void getsEmptyQueryParamStringWhenEmptyFromDate() {
        transactionSearchParams.setFromDate("");
        assertThat(transactionSearchParams.buildQueryParamString(1L), not(containsString("from_date")));
    }

    @Test
    public void getsURLEncodedQueryParamStringWhenNotEmptyFromDate() {
        transactionSearchParams.setFromDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionSearchParams.buildQueryParamString(1L), containsString("from_date=2018-09-22T10%3A14%3A16.067Z"));
    }

    @Test
    public void getsEmptyQueryParamStringWhenEmptyToDate() {
        transactionSearchParams.setToDate("");
        assertThat(transactionSearchParams.buildQueryParamString(1L), not(containsString("to_date")));
    }

    @Test
    public void getsURLEncodedQueryParamStringWhenNotEmptyToDate() {
        transactionSearchParams.setToDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionSearchParams.buildQueryParamString(1L), containsString("to_date=2018-09-22T10%3A14%3A16.067Z"));
    }

    @Test
    public void getLimitTotalSizeShouldReturnDefaultValueIfBelowDisplaySize() {
        transactionSearchParams.setDisplaySize(100L);
        transactionSearchParams.setLimitTotalSize(10L);

        assertThat(transactionSearchParams.getLimitTotalSize(), is(10000L));
    }

    @Test
    public void shouldApplyFromSettledDateCorrectly() {
        transactionSearchParams.setFromSettledDate("2020-09-25");
        assertThat(transactionSearchParams.getQueryMap().get("from_settled_date"), is(ZonedDateTime.parse("2020-09-25T00:00:00.000Z")));
        assertThat(transactionSearchParams.buildQueryParamString(1L), containsString("from_settled_date=2020-09-25"));
    }

    @Test
    public void shouldApplyToSettledDateCorrectly() {
        transactionSearchParams.setToSettledDate("2020-09-26");
        assertThat(transactionSearchParams.getQueryMap().get("to_settled_date"), is(ZonedDateTime.parse("2020-09-27T00:00:00.000Z")));
        assertThat(transactionSearchParams.buildQueryParamString(1L), containsString("to_settled_date=2020-09-26"));
    }

    @ParameterizedTest
    @CsvSource({"?, %3F", 
            "{ , %7B", 
            "[ , %5B", 
            "f{o{o}{ , f%7Bo%7Bo%7D%7B", 
            "foo&, foo%26", 
            "foo@ , foo%40", 
            "foo@@ , foo%40%40", 
            "foo=bar&baz=quux , foo%3Dbar%26baz%3Dquux", 
            "foo bar , foo+bar"
    })
    public void shouldUrlEncodeOnlyPrescribedSpecialCharactersOnly(String key, String value){
        transactionSearchParams.setReference(key);
        String expectedSelfLink = "reference=" + value + "&page=1&display_size=500";
        assertEquals(transactionSearchParams.buildQueryParamString(1L), expectedSelfLink);
    }
}
