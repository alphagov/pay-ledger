package uk.gov.pay.ledger.transaction.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.dao.TransactionSearchDao;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;

import javax.ws.rs.core.UriInfo;
import java.util.List;

public class TransactionSearchService {

    private final TransactionSearchDao transactionSearchDao;

    @Inject
    public TransactionSearchService(TransactionSearchDao transactionDao) {
        this.transactionSearchDao = transactionDao;
    }

    public TransactionSearchResponse searchTransactions(TransactionSearchParams searchParams, UriInfo uriInfo) {
        List<TransactionView> transactionViewList = transactionSearchDao.searchTransactionView(searchParams);
        Long total = transactionSearchDao.getTotalForSearch(searchParams);
        return new TransactionSearchResponse(searchParams.getAccountId(), total, (long)transactionViewList.size(),
                searchParams.getPageNumber(), transactionViewList);
    }
}
