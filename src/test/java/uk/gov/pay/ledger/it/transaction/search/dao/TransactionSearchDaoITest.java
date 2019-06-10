package uk.gov.pay.ledger.it.transaction.search.dao;


import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.pay.ledger.rules.AppWithPostgresRule;
import uk.gov.pay.ledger.transaction.search.common.CommaDelimitedSetParameter;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.dao.TransactionSearchDao;
import uk.gov.pay.ledger.transaction.model.CardDetails;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.utils.fixtures.TransactionFixture;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.ledger.utils.fixtures.TransactionFixture.aTransactionFixture;


public class TransactionSearchDaoITest {

    @ClassRule
    public static AppWithPostgresRule rule = new AppWithPostgresRule();

    private TransactionFixture transactionFixture;
    private TransactionSearchDao transactionSearchDao;
    private TransactionSearchParams searchParams;

    @Before
    public void setUp() {
        transactionSearchDao = new TransactionSearchDao(rule.getJdbi());
        searchParams = new TransactionSearchParams();
    }

    @Test
    public void shouldGetAndMapTransactionViewCorrectly() {

        transactionFixture = aTransactionFixture()
                .insert(rule.getJdbi());

        searchParams.setAccountId(transactionFixture.getGatewayAccountId());

        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(1));
        TransactionView transactionView = viewList.get(0);

        assertThat(transactionView.getId(), is(transactionFixture.getId()));
        assertThat(transactionView.getGatewayAccountId(), is(transactionFixture.getGatewayAccountId()));
        assertThat(transactionView.getAmount(), is(transactionFixture.getAmount()));
        assertThat(transactionView.getState(), is(TransactionState.valueOf(transactionFixture.getState().toUpperCase())));
        assertThat(transactionView.getReference(), is(transactionFixture.getReference()));
        assertThat(transactionView.getDescription(), is(transactionFixture.getDescription()));
        assertThat(transactionView.getLanguage(), is(transactionFixture.getLanguage()));
        assertThat(transactionView.getReturnUrl(), is(transactionFixture.getReturnUrl()));
        assertThat(transactionView.getExternalId(), is(transactionFixture.getExternalId()));
        assertThat(transactionView.getEmail(), is(transactionFixture.getEmail()));
        assertThat(transactionView.getPaymentProvider(), is(transactionFixture.getPaymentProvider()));
        assertThat(transactionView.getCreatedDate(), is(transactionFixture.getCreatedAt()));
        assertThat(transactionView.getDelayedCapture(), is(transactionFixture.getDelayedCapture()));

        assertThat(transactionView.getCardDetails().getCardHolderName(), is(transactionFixture.getCardDetails().getCardHolderName()));
        assertThat(transactionView.getCardDetails().getCardBrand(), is(transactionFixture.getCardDetails().getCardBrand()));

        assertThat(transactionView.getCardDetails().getBillingAddress().getAddressLine1(), is(transactionFixture.getCardDetails().getBillingAddress().getAddressLine1()));
        assertThat(transactionView.getCardDetails().getBillingAddress().getAddressLine2(), is(transactionFixture.getCardDetails().getBillingAddress().getAddressLine2()));
        assertThat(transactionView.getCardDetails().getBillingAddress().getAddressCounty(), is(transactionFixture.getCardDetails().getBillingAddress().getAddressCounty()));
        assertThat(transactionView.getCardDetails().getBillingAddress().getAddressCity(), is(transactionFixture.getCardDetails().getBillingAddress().getAddressCity()));
        assertThat(transactionView.getCardDetails().getBillingAddress().getAddressPostCode(), is(transactionFixture.getCardDetails().getBillingAddress().getAddressPostCode()));
        assertThat(transactionView.getCardDetails().getBillingAddress().getAddressCountry(), is(transactionFixture.getCardDetails().getBillingAddress().getAddressCountry()));
    }

    @Test
    public void shouldReturn2Records_whenSearchingBySpecificGatewayAccountId() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .insert(rule.getJdbi());
        }
        aTransactionFixture()
                .insert(rule.getJdbi());

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);

        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(2));
        assertThat(viewList.get(0).getGatewayAccountId(), is(gatewayAccountId));
        assertThat(viewList.get(1).getGatewayAccountId(), is(gatewayAccountId));
    }

    @Test
    public void shouldReturn1Record_whenSearchingByEmail() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withEmail("testemail" + i + "@example.org")
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setEmail("testemail1");

        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(1));
        assertThat(viewList.get(0).getEmail(), is("testemail1@example.org"));
    }

    @Test
    public void shouldReturn1Record_whenSearchingByReference() {

        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withId((long) i)
                    .withAmount(100l + i)
                    .withGatewayAccountId(gatewayAccountId)
                    .withReference("reference " + i)
                    .withDescription("description " + i)
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setReference("reference 1");

        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(1));
        assertThat(viewList.get(0).getReference(), is("reference 1"));
    }

    @Test
    public void shouldReturn1Record_whenSearchingByCardHolderName() {

        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            CardDetails cardDetails = new CardDetails("name" + i, null, null);

            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withReference("reference " + i)
                    .withCardDetails(cardDetails)
                    .withDescription("description " + i)
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setCardHolderName("name1");

        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(1));
        assertThat(viewList.get(0).getCardDetails().getCardHolderName(), is("name1"));
    }

    @Test
    public void shouldReturn1Record_withFromDateSet() {

        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 1; i < 4; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withCreatedDate(ZonedDateTime.now().minusDays(i))
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setFromDate(ZonedDateTime.now().minusDays(1).minusMinutes(10));

        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(1));
    }

    @Test
    public void shouldReturn2Records_withToDateSet() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 1; i < 4; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withCreatedDate(ZonedDateTime.now().minusDays(i))
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setToDate(ZonedDateTime.now().minusDays(2).plusMinutes(10));

        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(2));
    }

    @Test
    public void shouldReturn10Records_withPagesize10() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 1; i < 20; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setDisplaySize(10l);

        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(10));
    }

    @Test
    public void shouldReturn2Records_withOffsetAndPagesizeSet() {
        String gatewayAccountId = "account-id-" + nextLong();
        long id = nextLong();

        for (int i = 1; i < 20; i++) {
            aTransactionFixture()
                    .withId(id + i)
                    .withGatewayAccountId(gatewayAccountId)
                    .withReference("reference" + i)
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setDisplaySize(2l);
        searchParams.setPageNumber(3l);


        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(2));
        assertThat(viewList.get(0).getReference(), is("reference15"));
        assertThat(viewList.get(1).getReference(), is("reference14"));
    }

    @Test
    public void shouldReturn2Records_WhenSearchingByCreatedState() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .insert(rule.getJdbi());
        }
        aTransactionFixture()
                .withState("random-state")
                .withGatewayAccountId(gatewayAccountId)
                .insert(rule.getJdbi());

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setPaymentStates(new CommaDelimitedSetParameter("created"));

        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(2));
    }

    @Test
    public void shouldReturnNoRecords_WhenStateIsOtherThanCreated() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withState("random-state")
                    .insert(rule.getJdbi());
        }
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setPaymentStates(new CommaDelimitedSetParameter("random-state"));

        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(0));
    }

    @Test
    public void shouldReturnNoRecords_whenRefundStatesAreSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withState("random-refund-state")
                    .insert(rule.getJdbi());
        }
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setRefundStates(new CommaDelimitedSetParameter("random-refund-state"));

        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(0));
    }

    //todo: modify test to return results when last digits card number is available in DB
    @Test
    public void shouldReturnNoRecords_whenLastDigitsCardNumberIsSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .insert(rule.getJdbi());
        }
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setLastDigitsCardNumber("1234");

        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(0));
    }

    //todo: modify test to return results when first digits card number is available in DB
    @Test
    public void shouldReturnNoRecords_whenFirstDigitsCardNumberIsSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .insert(rule.getJdbi());
        }
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setFirstDigitsCardNumber("1234");

        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(0));
    }

    //todo: modify test to return results when card_brand is available in DB
    @Test
    public void shouldReturnNoRecords_whenCardBrandIsSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .insert(rule.getJdbi());
        }
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setCardBrands(Arrays.asList("random-card-brand"));

        List<TransactionView> viewList = transactionSearchDao.searchTransactionView(searchParams);

        assertThat(viewList.size(), Matchers.is(0));
    }
}