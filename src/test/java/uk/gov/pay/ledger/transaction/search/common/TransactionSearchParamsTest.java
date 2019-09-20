package uk.gov.pay.ledger.transaction.search.common;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.ledger.transaction.model.TransactionType;

import java.util.List;

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
    public void getFilterTemplateWithParentTxnSearch_shouldReturnFilterWithReferenceSearch() {
        transactionSearchParams.setReference("test-reference");
        assertThat(transactionSearchParams.getFilterTemplatesWithParentTransactionSearch().get(0),
                is(" (lower(t.reference) like lower(:reference) or lower(parent.reference) like lower(:reference))"));
        assertThat(transactionSearchParams.getQueryMap().get("reference"), is("%test-reference%"));
    }

    @Test
    public void getFilterTemplateWithParentTxnSearch_shouldReturnFilterWithExactReferenceSearch() {
        transactionSearchParams.setReference("test-reference");
        transactionSearchParams.setExactReferenceMatch(true);
        assertThat(transactionSearchParams.getFilterTemplatesWithParentTransactionSearch().get(0),
                is(" (lower(t.reference) = lower(:reference) or lower(parent.reference) = lower(:reference))"));
        assertThat(transactionSearchParams.getQueryMap().get("reference"), is("test-reference"));
    }

    @Test
    public void getFilterTemplateWithParentTxnSearch_shouldReturnFilterWithEmail() {
        transactionSearchParams.setEmail("test-email");
        assertThat(transactionSearchParams.getFilterTemplatesWithParentTransactionSearch().get(0),
                is(" (lower(t.email) like lower(:email) or lower(parent.email) like lower(:email))"));
        assertThat(transactionSearchParams.getQueryMap().get("email"), is("%test-email%"));
    }

    @Test
    public void getFilterTemplateWithParentTxnSearch_shouldReturnFilterWithCardholderName() {
        transactionSearchParams.setCardHolderName("test-name");
        assertThat(transactionSearchParams.getFilterTemplatesWithParentTransactionSearch().get(0),
                is(" (lower(t.cardholder_name) like lower(:cardholder_name) or lower(parent.cardholder_name) like lower(:cardholder_name))"));
        assertThat(transactionSearchParams.getQueryMap().get("cardholder_name"), is("%test-name%"));
    }

    @Test
    public void getFilterTemplateWithParentTxnSearch_shouldReturnFilterWithLastDigitsCardNumber() {
        transactionSearchParams.setLastDigitsCardNumber("123456");
        assertThat(transactionSearchParams.getFilterTemplatesWithParentTransactionSearch().get(0),
                is(" (t.last_digits_card_number = :last_digits_card_number or parent.last_digits_card_number = :last_digits_card_number)"));
        assertThat(transactionSearchParams.getQueryMap().get("last_digits_card_number"), is("123456"));
    }

    @Test
    public void getFilterTemplateWithParentTxnSearch_shouldReturnFilterWithCardBrand() {
        transactionSearchParams.setCardBrands(new CommaDelimitedSetParameter("visa,mastercard"));
        assertThat(transactionSearchParams.getFilterTemplatesWithParentTransactionSearch().get(0),
                is(" (lower(t.card_brand) IN (<card_brand>) or lower(parent.card_brand) IN (<card_brand>))"));
        List<String> cardBrand = (List<String>) transactionSearchParams.getQueryMap().get("card_brand");
        assertThat(cardBrand.get(0), is("visa"));
        assertThat(cardBrand.get(1), is("mastercard"));
    }

    @Test
    public void getFilterTemplateWithParentTxnSearch_shouldReturnFilterWithFromDate() {
        transactionSearchParams.setFromDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionSearchParams.getFilterTemplatesWithParentTransactionSearch().get(0), is(" t.created_date > :from_date"));
        assertThat(transactionSearchParams.getQueryMap().get("from_date").toString(), is("2018-09-22T10:14:16.067Z"));
    }

    @Test
    public void getFilterTemplateWithParentTxnSearch_shouldReturnFilterWithToDate() {
        transactionSearchParams.setToDate("2018-09-22T10:14:16.067Z");
        assertThat(transactionSearchParams.getFilterTemplatesWithParentTransactionSearch().get(0), is(" t.created_date < :to_date"));
        assertThat(transactionSearchParams.getQueryMap().get("to_date").toString(), is("2018-09-22T10:14:16.067Z"));
    }

    @Test
    public void getFilterTemplateWithParentTxnSearch_shouldReturnFilterWithGatewayAccountId() {
        transactionSearchParams.setAccountId("1");
        assertThat(transactionSearchParams.getFilterTemplatesWithParentTransactionSearch().get(0), is(" t.gateway_account_id = :account_id"));
        assertThat(transactionSearchParams.getQueryMap().get("account_id"), is("1"));
    }

    @Test
    public void getFilterTemplateWithParentTxnSearch_shouldReturnFilterWithType() {
        transactionSearchParams.setTransactionType(TransactionType.PAYMENT);
        assertThat(transactionSearchParams.getFilterTemplatesWithParentTransactionSearch().get(0), is(" t.type = :transaction_type::transaction_type"));
        assertThat(transactionSearchParams.getQueryMap().get("transaction_type"), is(TransactionType.PAYMENT));
    }

    @Test
    public void getFilterTemplateWithParentTxnSearch_shouldReturnFilterWithFirstDigitsCardNumber() {
        transactionSearchParams.setFirstDigitsCardNumber("1234");
        assertThat(transactionSearchParams.getFilterTemplatesWithParentTransactionSearch().get(0), is(" t.first_digits_card_number = :first_digits_card_number"));
        assertThat(transactionSearchParams.getQueryMap().get("first_digits_card_number"), is("1234"));
    }

    @Test
    public void getFilterTemplateWithParentTxnSearch_shouldReturnFilterWithState() {
        transactionSearchParams.setState("success");
        assertThat(transactionSearchParams.getFilterTemplatesWithParentTransactionSearch().get(0), is(" t.state IN (<state>)"));
        assertThat(((List) transactionSearchParams.getQueryMap().get("state")).get(0), is("SUCCESS"));
    }

    @Test
    public void getFilterTemplateWithParentTxnSearch_shouldReturnFilterWithPaymentState() {
        transactionSearchParams.setPaymentStates(new CommaDelimitedSetParameter("success"));
        assertThat(transactionSearchParams.getFilterTemplatesWithParentTransactionSearch().get(0), is("( (t.state IN (<payment_states>) AND t.type =  'PAYMENT'::transaction_type))"));
        assertThat(((List) transactionSearchParams.getQueryMap().get("payment_states")).get(0), is("SUCCESS"));
    }

    @Test
    public void getFilterTemplateWithParentTxnSearch_shouldReturnFilterWithRefundState() {
        transactionSearchParams.setRefundStates(new CommaDelimitedSetParameter("success"));
        assertThat(transactionSearchParams.getFilterTemplatesWithParentTransactionSearch().get(0), is("( (t.state IN (<refund_states>) AND t.type =  'REFUND'::transaction_type))"));
        assertThat(((List) transactionSearchParams.getQueryMap().get("refund_states")).get(0), is("SUCCESS"));
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