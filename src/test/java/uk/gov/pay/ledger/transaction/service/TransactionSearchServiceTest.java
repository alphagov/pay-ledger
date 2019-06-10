package uk.gov.pay.ledger.transaction.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.dao.TransactionSearchDao;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.utils.fixtures.TransactionViewFixture;

import javax.ws.rs.core.UriInfo;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransactionSearchServiceTest {

    @Mock
    private TransactionSearchDao mockTransactionSearchDao;
    @Mock
    private UriInfo mockUriInfo;

    private TransactionSearchService transactionSearchService;

    @Before
    public void setUp() {
        transactionSearchService = new TransactionSearchService(mockTransactionSearchDao);
    }

    @Test
    public void shouldReturnAListOfTransactions() {
        String gatewayAccountId = "gateway_account_id";
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        List<TransactionView> transactionViewList = TransactionViewFixture.aTransactionViewList(gatewayAccountId, 5);
        when(mockTransactionSearchDao.searchTransactionView(any(TransactionSearchParams.class))).thenReturn(transactionViewList);
        when(mockTransactionSearchDao.getTotalForSearch(any(TransactionSearchParams.class))).thenReturn(5L);
        TransactionSearchResponse transactionSearchResponse = transactionSearchService.searchTransactions(searchParams, mockUriInfo);
        assertThat(transactionSearchResponse.getGatewayExternalId(), is(gatewayAccountId));
        assertThat(transactionSearchResponse.getPage(), is(1L));
        assertThat(transactionSearchResponse.getCount(), is(5L));
        assertThat(transactionSearchResponse.getTotal(), is(5L));
        assertThat(transactionSearchResponse.getTransactionViewList().size(), is(5));
    }
}