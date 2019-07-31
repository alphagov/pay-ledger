package uk.gov.pay.ledger.transaction.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.PaymentFactory;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.model.TransactionEvent;
import uk.gov.pay.ledger.transaction.model.TransactionEventResponse;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.search.common.HalLinkBuilder;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.model.Link;
import uk.gov.pay.ledger.transaction.search.model.PaginationBuilder;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;

public class TransactionService {

    private final TransactionDao transactionDao;
    private final EventDao eventDao;
    private TransactionEntityFactory transactionEntityFactory;
    private PaymentFactory paymentFactory;

    @Inject
    public TransactionService(TransactionDao transactionDao, EventDao eventDao, TransactionEntityFactory transactionEntityFactory,
                              PaymentFactory paymentFactory) {
        this.transactionDao = transactionDao;
        this.eventDao = eventDao;
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
        List<Transaction> transactionList = transactionDao.searchTransactions(searchParams)
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

    // @TODO(sfount) handling writing invalid transaction should be tested at `EventMessageHandler` integration level
    public void upsertTransactionFor(EventDigest eventDigest) {
        TransactionEntity transaction = transactionEntityFactory.create(eventDigest);
        transactionDao.upsert(transaction);
    }

    public TransactionEventResponse findTransactionEvents(String externalId, String gatewayAccountId,
                                                          boolean includeAllEvents) {
        Map<String, TransactionEntity> transactionEntityMap = getTransactionsAsMap(externalId, gatewayAccountId);

        if (transactionEntityMap.isEmpty()) {
            throw new BadRequestException(format("Transaction with id [%s] not found", externalId));
        }

        List<TransactionEvent> transactionEvents = getTransactionEventsFor(transactionEntityMap);

        if (includeAllEvents) {
            return TransactionEventResponse.of(externalId, transactionEvents);
        } else {
            return TransactionEventResponse.of(externalId, removeDuplicates(transactionEvents));
        }
    }

    private Map<String, TransactionEntity> getTransactionsAsMap(String externalId, String gatewayAccountId) {
        return transactionDao.findTransactionByExternalOrParentIdAndGatewayAccountId(
                externalId, gatewayAccountId)
                .stream()
                .collect(Collectors.toMap(TransactionEntity::getExternalId,
                        transactionEntity -> transactionEntity));
    }

    private List<TransactionEvent> getTransactionEventsFor(Map<String, TransactionEntity> transactionEntityMap) {
        List<Event> events = eventDao.findEventsForExternalIds(transactionEntityMap.keySet());
        return mapToTransactionEvent(transactionEntityMap, events);
    }

    private List<TransactionEvent> mapToTransactionEvent(Map<String, TransactionEntity> transactionEntityMap, List<Event> eventList) {
        return eventList.stream()
                .map(event -> TransactionEvent.from(transactionEntityMap.get(event.getResourceExternalId()), event))
                .collect(Collectors.toList());
    }

    private List<TransactionEvent> removeDuplicates(List<TransactionEvent> transactionEvents) {
        // removes 1. events without mapping to transaction state
        // 2. duplicate events based on external_id,resource_type & state (gets the first created event based on event date)
        Collection<Optional<TransactionEvent>> values = transactionEvents.stream()
                .filter(transactionEvent -> transactionEvent.getState() != null)
                .collect(groupingBy(transactionEvent -> transactionEvent.getExternalId() + transactionEvent.getResourceType() + transactionEvent.getState(),
                        Collectors.minBy(Comparator.comparing(TransactionEvent::getTimestamp))))
                .values();

        return values.stream().filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(TransactionEvent::getTimestamp))
                .collect(Collectors.toList());
    }

}
