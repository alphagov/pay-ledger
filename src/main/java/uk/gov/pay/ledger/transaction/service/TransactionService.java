package uk.gov.pay.ledger.transaction.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.Payment;
import uk.gov.pay.ledger.transaction.model.PaymentFactory;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.search.common.HalLinkBuilder;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.model.Link;
import uk.gov.pay.ledger.transaction.search.model.PaginationBuilder;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.transaction.state.TransactionState;

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

    public Optional<TransactionView> getTransaction(String transactionExternalId, UriInfo uriInfo) {
            return transactionDao.findTransactionByExternalId(transactionExternalId)
                    .map(entity -> decorateWithLinks(TransactionView.from(paymentFactory.fromTransactionEntity(entity)), uriInfo));
    }

    public TransactionSearchResponse searchTransactions(TransactionSearchParams searchParams, UriInfo uriInfo) {
        List<Payment> transactionList = transactionDao.searchTransactions(searchParams)
                .stream()
                .map(paymentFactory::fromTransactionEntity)
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

        // That's NOT what's ChargeService is checking when "populateResponseBuilderWith" - should be done properly
        if (TransactionState.SUBMITTED.equals(transactionView.getState()) && transactionView.getDelayedCapture()) {
            Link captureLink = HalLinkBuilder.createCaptureLink(uriInfo, "/v1/transaction/{externalId}/capture",
                    transactionView.getExternalId());
            transactionView.addLink(captureLink);
        }

        //!chargeStatus.toExternal().isFinished() && !chargeStatus.equals(AWAITING_CAPTURE_REQUEST);
        if(!transactionView.getState().isFinished() && transactionView.getPaymentLinks() != null) {
            transactionView.addLink(transactionView.getPaymentLinks().stream().filter(l -> l.getRel().equals("next_url")).findFirst().orElse(null));
            transactionView.addLink(transactionView.getPaymentLinks().stream().filter(l -> l.getRel().equals("next_url_post")).findFirst().orElse(null));
        }

        return transactionView;
    }

    public void upsertTransactionFor(EventDigest eventDigest) {
        TransactionEntity transaction = transactionEntityFactory.from(eventDigest);
        transactionDao.upsert(transaction);
    }
}
