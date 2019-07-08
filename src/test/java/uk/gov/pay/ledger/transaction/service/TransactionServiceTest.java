package uk.gov.pay.ledger.transaction.service;

import io.dropwizard.jackson.Jackson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.search.common.CommaDelimitedSetParameter;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.model.PaginationBuilder;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceTest {

    @Mock
    private TransactionDao mockTransactionDao;
    @Mock
    private UriInfo mockUriInfo;

    private TransactionService transactionService;
    private String gatewayAccountId = "gateway_account_id";
    private TransactionSearchParams searchParams;

    @Before
    public void setUp() {
        TransactionEntityFactory transactionEntityFactory = new TransactionEntityFactory(Jackson.newObjectMapper());
        transactionService = new TransactionService(mockTransactionDao, transactionEntityFactory);
        searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);

        when(mockUriInfo.getBaseUri()).thenReturn(UriBuilder.fromUri("http://app.com").build());
        when(mockUriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri("http://app.com"));
        when(mockUriInfo.getPath()).thenReturn("/v1/transaction");
    }

    @Test
    public void shouldReturnAListOfTransactions() {
        List<TransactionEntity> transactionViewList = TransactionFixture.aTransactionList(gatewayAccountId, 5);
        when(mockTransactionDao.searchTransactions(any(TransactionSearchParams.class))).thenReturn(transactionViewList);
        when(mockTransactionDao.getTotalForSearch(any(TransactionSearchParams.class))).thenReturn(5L);
        TransactionSearchResponse transactionSearchResponse = transactionService.searchTransactions(searchParams, mockUriInfo);
        assertThat(transactionSearchResponse.getGatewayExternalId(), is(gatewayAccountId));
        assertThat(transactionSearchResponse.getPage(), is(1L));
        assertThat(transactionSearchResponse.getCount(), is(5L));
        assertThat(transactionSearchResponse.getTotal(), is(5L));
        assertThat(transactionSearchResponse.getTransactionViewList().size(), is(5));
    }

    @Test
    public void shouldListTransactionWithCorrectSelfAndRefundsLinks() {
        List<TransactionEntity> transactionViewList = TransactionFixture.aTransactionList(gatewayAccountId, 1);
        when(mockTransactionDao.searchTransactions(any(TransactionSearchParams.class))).thenReturn(transactionViewList);
        when(mockTransactionDao.getTotalForSearch(any(TransactionSearchParams.class))).thenReturn(1L);

        TransactionSearchResponse transactionSearchResponse = transactionService.searchTransactions(searchParams, mockUriInfo);
        TransactionView transactionView = transactionSearchResponse.getTransactionViewList().get(0);

        assertThat(transactionView.getLinks().get(0).getRel(), is("self"));
        assertThat(transactionView.getLinks().get(0).getMethod(), is("GET"));
        assertThat(transactionView.getLinks().get(0).getHref(), is("http://app.com/v1/transaction/" + transactionView.getExternalId()));

        assertThat(transactionView.getLinks().get(1).getRel(), is("refunds"));
        assertThat(transactionView.getLinks().get(1).getMethod(), is("GET"));
        assertThat(transactionView.getLinks().get(1).getHref(), is("http://app.com/v1/transaction/" + transactionView.getExternalId() + "/refunds"));
    }

    @Test
    public void shouldListTransactionsWithAllPaginationLinks() {
        List<TransactionEntity> transactionViewList = TransactionFixture
                .aTransactionList(gatewayAccountId, 100);
        searchParams.setPageNumber(3l);
        searchParams.setDisplaySize(10l);
        when(mockTransactionDao.searchTransactions(any(TransactionSearchParams.class))).thenReturn(transactionViewList);
        when(mockTransactionDao.getTotalForSearch(any(TransactionSearchParams.class))).thenReturn(100L);

        TransactionSearchResponse transactionSearchResponse = transactionService.searchTransactions(searchParams, mockUriInfo);
        PaginationBuilder paginationBuilder = transactionSearchResponse.getPaginationBuilder();

        assertThat(paginationBuilder.getFirstLink().getHref(), is("http://app.com/v1/transaction?account_id=gateway_account_id&page=1&display_size=10"));
        assertThat(paginationBuilder.getPrevLink().getHref(), is("http://app.com/v1/transaction?account_id=gateway_account_id&page=2&display_size=10"));
        assertThat(paginationBuilder.getSelfLink().getHref(), is("http://app.com/v1/transaction?account_id=gateway_account_id&page=3&display_size=10"));
        assertThat(paginationBuilder.getNextLink().getHref(), is("http://app.com/v1/transaction?account_id=gateway_account_id&page=4&display_size=10"));
        assertThat(paginationBuilder.getLastLink().getHref(), is("http://app.com/v1/transaction?account_id=gateway_account_id&page=10&display_size=10"));
    }

    @Test
    public void shouldListTransactionsWithCorrectQueryParamsForPaginationLinks() {
        List<TransactionEntity> transactionViewList = TransactionFixture.aTransactionList(gatewayAccountId, 10);
        when(mockTransactionDao.searchTransactions(any(TransactionSearchParams.class))).thenReturn(transactionViewList);
        when(mockTransactionDao.getTotalForSearch(any(TransactionSearchParams.class))).thenReturn(10L);

        searchParams.setEmail("test@email.com");
        searchParams.setCardHolderName("test");
        searchParams.setFromDate("2019-05-01T10:15:30Z");
        searchParams.setToDate("2019-06-01T10:15:30Z");
        searchParams.setReference("ref");
        searchParams.setFirstDigitsCardNumber("4242");
        searchParams.setLastDigitsCardNumber("1234");
        searchParams.setPaymentStates(new CommaDelimitedSetParameter("created,submitted"));
        searchParams.setRefundStates(new CommaDelimitedSetParameter("created,refunded"));
        searchParams.setCardBrands(new CommaDelimitedSetParameter("visa,mastercard"));

        TransactionSearchResponse transactionSearchResponse = transactionService.searchTransactions(searchParams, mockUriInfo);
        PaginationBuilder paginationBuilder = transactionSearchResponse.getPaginationBuilder();
        String selfLink = paginationBuilder.getSelfLink().getHref();

        assertThat(selfLink, containsString("email=test%40email.com"));
        assertThat(selfLink, containsString("reference=ref"));
        assertThat(selfLink, containsString("cardholder_name=test"));
        assertThat(selfLink, containsString("first_digits_card_number=4242"));
        assertThat(selfLink, containsString("last_digits_card_number=1234"));
        assertThat(selfLink, containsString("payment_states=created%2Csubmitted"));
        assertThat(selfLink, containsString("refund_states=created%2Crefunded"));
        assertThat(selfLink, containsString("card_brand=visa%2Cmastercard"));
    }
}