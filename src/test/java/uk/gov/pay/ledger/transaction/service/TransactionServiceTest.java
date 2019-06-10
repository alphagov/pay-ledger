package uk.gov.pay.ledger.transaction.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.utils.fixtures.TransactionFixture;

import javax.ws.rs.core.UriInfo;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceTest {

    @Mock
    private TransactionDao mockTransactionDao;
    @Mock
    private UriInfo mockUriInfo;

    private TransactionService transactionService;

    @Before
    public void setUp() {
        transactionService = new TransactionService(mockTransactionDao);
    }

    @Test
    public void shouldReturnAListOfTransactions() {
        String gatewayAccountId = "gateway_account_id";
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        List<Transaction> transactionViewList = TransactionFixture.aTransactionList(gatewayAccountId, 5);
        when(mockTransactionDao.searchTransactions(any(TransactionSearchParams.class))).thenReturn(transactionViewList);
        when(mockTransactionDao.getTotalForSearch(any(TransactionSearchParams.class))).thenReturn(5L);
        TransactionSearchResponse transactionSearchResponse = transactionService.searchTransactions(searchParams, mockUriInfo);
        assertThat(transactionSearchResponse.getGatewayExternalId(), is(gatewayAccountId));
        assertThat(transactionSearchResponse.getPage(), is(1L));
        assertThat(transactionSearchResponse.getCount(), is(5L));
        assertThat(transactionSearchResponse.getTotal(), is(5L));
        assertThat(transactionSearchResponse.getTransactionViewList().size(), is(5));
    }
}