package uk.gov.pay.ledger.transaction.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.search.model.Link;
import uk.gov.pay.ledger.transaction.model.Payment;
import uk.gov.pay.ledger.transaction.model.PaymentFactory;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.search.common.HalLinkBuilder;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.model.PaginationBuilder;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;

import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TransactionService {

    private final TransactionDao transactionDao;
    private TransactionEntityFactory transactionEntityFactory;
    private PaymentFactory paymentFactory;

    @Inject
    public TransactionService(TransactionDao transactionDao, TransactionEntityFactory transactionEntityFactory,
                              PaymentFactory paymentFactory) {
        this.transactionDao = transactionDao;
        this.transactionEntityFactory = transactionEntityFactory;
        this.paymentFactory = paymentFactory;
    }

    public Optional<TransactionView> getTransactionForGatewayAccount(String gatewayAccountId, String transactionExternalId, UriInfo uriInfo) {
        return transactionDao.findTransactionByExternalIdAndGatewayAccountId(transactionExternalId, gatewayAccountId)
                .map(entity -> decorateWithLinks(TransactionView.from(paymentFactory.createTransactionEntity(entity)), uriInfo));
    }

    public Optional<TransactionView> getTransaction(String transactionExternalId, UriInfo uriInfo) {
            return transactionDao.findTransactionByExternalId(transactionExternalId)
                    .map(entity -> decorateWithLinks(TransactionView.from(paymentFactory.createTransactionEntity(entity)), uriInfo));
    }

    public TransactionSearchResponse searchTransactions(TransactionSearchParams searchParams, UriInfo uriInfo) {
        List<Payment> transactionList = transactionDao.searchTransactions(searchParams)
                .stream()
                .map(paymentFactory::createTransactionEntity)
                .collect(Collectors.toList());
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

    private List<TransactionView> mapToTransactionViewList(List<Payment> transactionList, TransactionSearchParams searchParams,
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

    // @TODO(sfount) handling writing invalid transaction should be tested at `EventMessageHandler` integration level
    public void upsertTransactionFor(EventDigest eventDigest) {
        TransactionEntity transaction = transactionEntityFactory.create(eventDigest);
        transactionDao.upsert(transaction);
    }
}
