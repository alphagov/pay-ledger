package uk.gov.pay.ledger.transaction.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;

import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionService {

    private final TransactionDao transactionDao;

    @Inject
    public TransactionService(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    public TransactionSearchResponse searchTransactions(TransactionSearchParams searchParams, UriInfo uriInfo) {
        List<Transaction> transactionList = transactionDao.searchTransactions(searchParams);
        Long total = transactionDao.getTotalForSearch(searchParams);
        return new TransactionSearchResponse(searchParams.getAccountId(), total, (long)transactionList.size(),
                searchParams.getPageNumber(),
                transactionList.stream()
                    .map(transaction -> TransactionView.from(transaction))
                    .collect(Collectors.toList()));
    }
}
