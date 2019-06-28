package uk.gov.pay.ledger.transaction.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.model.Address;
import uk.gov.pay.ledger.transaction.model.CardDetails;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.search.common.HalLinkBuilder;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.model.Link;
import uk.gov.pay.ledger.transaction.search.model.PaginationBuilder;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.transaction.state.TransactionState;

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

        PaginationBuilder paginationBuilder = new PaginationBuilder(searchParams, uriInfo);
        paginationBuilder = paginationBuilder.withTotalCount(total).buildResponse();

        List<TransactionView> transactionViewList = mapToTransactionViewList(transactionList, searchParams, uriInfo);

        return new TransactionSearchResponse(searchParams.getAccountId(),
                total,
                (long) transactionList.size(),
                searchParams.getPageNumber(),
                transactionViewList
        ).withPaginationBuilder(paginationBuilder);
    }

    private List<TransactionView> mapToTransactionViewList(List<Transaction> transactionList, TransactionSearchParams searchParams,
                                                           UriInfo uriInfo) {
        return transactionList.stream()
                .map(transaction -> decorateWithLinks(TransactionView.from(transaction),
                        uriInfo))
                .collect(Collectors.toList());
    }

    private TransactionView decorateWithLinks(TransactionView transactionView,
                                              UriInfo uriInfo) {

        Link selfLink = HalLinkBuilder.createSelfLink(uriInfo, "/v1/transaction/{externalId}",
                transactionView.getExternalId());
        transactionView.addLink(selfLink);

        Link refundsLink = HalLinkBuilder.createRefundsLink(uriInfo, "/v1/transaction/{externalId}/refunds",
                transactionView.getExternalId());
        transactionView.addLink(refundsLink);

        return transactionView;
    }

    public void upsertTransactionFor(EventDigest eventDigest) {
        Transaction transaction = convertToTransaction(eventDigest);
        transactionDao.upsert(transaction);
    }

    private Transaction convertToTransaction(EventDigest eventDigest) {
        Address address = new Address(
                eventDigest.getEventDetailsDigest().getAddressLine1(),
                eventDigest.getEventDetailsDigest().getAddressLine2(),
                eventDigest.getEventDetailsDigest().getAddressPostcode(),
                eventDigest.getEventDetailsDigest().getAddressCity(),
                eventDigest.getEventDetailsDigest().getAddressCounty(),
                eventDigest.getEventDetailsDigest().getAddressCountry()
        );

        CardDetails cardDetails = new CardDetails(
                eventDigest.getEventDetailsDigest().getCardholderName(),
                address,
                null
        );

        return new Transaction(
                eventDigest.getEventDetailsDigest().getGatewayAccountId(),
                eventDigest.getEventDetailsDigest().getAmount(),
                eventDigest.getEventDetailsDigest().getReference(),
                eventDigest.getEventDetailsDigest().getDescription(),
                TransactionState.fromSalientEventType(eventDigest.getMostRecentSalientEventType()),
                eventDigest.getEventDetailsDigest().getLanguage(),
                eventDigest.getResourceExternalId(),
                eventDigest.getEventDetailsDigest().getReturnUrl(),
                eventDigest.getEventDetailsDigest().getEmail(),
                eventDigest.getEventDetailsDigest().getPaymentProvider(),
                eventDigest.getMostRecentEventTimestamp(),
                cardDetails,
                eventDigest.getEventDetailsDigest().getDelayedCapture(),
                null,
                eventDigest.getEventCount()
        );
    }
}
