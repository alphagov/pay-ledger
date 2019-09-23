package uk.gov.pay.ledger.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.model.TransactionEvent;
import uk.gov.pay.ledger.transaction.model.TransactionEventResponse;
import uk.gov.pay.ledger.transaction.model.TransactionFactory;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.model.TransactionsForTransactionResponse;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.model.PaginationBuilder;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
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

    public static final int DEFAULT_STATUS_VERSION = 2;
    private final TransactionDao transactionDao;
    private final EventDao eventDao;
    private TransactionEntityFactory transactionEntityFactory;
    private TransactionFactory transactionFactory;
    private ObjectMapper objectMapper;

    @Inject
    public TransactionService(TransactionDao transactionDao, EventDao eventDao, TransactionEntityFactory transactionEntityFactory,
                              TransactionFactory transactionFactory,
                              ObjectMapper objectMapper) {
        this.transactionDao = transactionDao;
        this.eventDao = eventDao;
        this.transactionEntityFactory = transactionEntityFactory;
        this.transactionFactory = transactionFactory;
        this.objectMapper = objectMapper;
    }

    public Optional<TransactionView> getTransactionForGatewayAccount(String gatewayAccountId, String transactionExternalId,
                                                                     TransactionType transactionType, String parentTransactionExternalId,
                                                                     int statusVersion) {
        return transactionDao.findTransaction(transactionExternalId, gatewayAccountId, transactionType, parentTransactionExternalId)
                .map(entity -> TransactionView.from(transactionFactory.createTransactionEntity(entity), statusVersion));
    }

    public Optional<TransactionView> getTransaction(String transactionExternalId, int statusVersion) {
        return transactionDao.findTransactionByExternalId(transactionExternalId)
                .map(entity -> TransactionView.from(transactionFactory.createTransactionEntity(entity), statusVersion));
    }

    public TransactionsForTransactionResponse getTransactions(String parentTransactionExternalId, String gatewayAccountId) {
        return transactionDao.findTransactionByExternalId(parentTransactionExternalId)
                .map(transactionEntity ->
                        findTransactionsForParentExternalId(
                                transactionEntity.getExternalId(),
                                transactionEntity.getGatewayAccountId()))
                .orElseThrow(() ->
                        new WebApplicationException(format("Transaction with id [%s] not found", parentTransactionExternalId),
                                Response.Status.NOT_FOUND));
    }

    public TransactionSearchResponse searchTransactions(TransactionSearchParams searchParams, UriInfo uriInfo) {
        return searchTransactions(null, searchParams, uriInfo);
    }

    public TransactionSearchResponse searchTransactions(String gatewayAccountId, TransactionSearchParams searchParams, UriInfo uriInfo) {
        if (StringUtils.isNotBlank(gatewayAccountId)) {
            searchParams.setAccountId(gatewayAccountId);
        }

        if (searchParams.getWithParentTransaction()) {
            return searchTransactionsAndParent(searchParams, uriInfo);
        } else {
            return searchTransactionsWithoutParent(searchParams, uriInfo);
        }
    }

    private TransactionSearchResponse searchTransactionsWithoutParent(TransactionSearchParams searchParams, UriInfo uriInfo) {
        List<Transaction> transactionList = transactionDao.searchTransactions(searchParams)
                .stream()
                .map(transactionFactory::createTransactionEntity)
                .collect(Collectors.toList());

        Long total = transactionDao.getTotalForSearch(searchParams);

        return buildTransactionSearchResponse(searchParams, uriInfo, transactionList, total);
    }

    private TransactionSearchResponse searchTransactionsAndParent(TransactionSearchParams searchParams, UriInfo uriInfo) {
        List<Transaction> transactionList = transactionDao.searchTransactionsAndParent(searchParams)
                .stream()
                .map(transactionFactory::createTransactionEntity)
                .collect(Collectors.toList());

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);

        return buildTransactionSearchResponse(searchParams, uriInfo, transactionList, total);
    }

    private TransactionSearchResponse buildTransactionSearchResponse(TransactionSearchParams searchParams, UriInfo uriInfo, List<Transaction> transactionList, Long total) {
        PaginationBuilder paginationBuilder = new PaginationBuilder(searchParams, uriInfo);
        paginationBuilder = paginationBuilder.withTotalCount(total).buildResponse();

        List<TransactionView> transactionViewList = mapToTransactionViewList(transactionList, searchParams.getStatusVersion());

        return new TransactionSearchResponse(
                total,
                (long) transactionList.size(),
                searchParams.getPageNumber(),
                transactionViewList
        ).withPaginationBuilder(paginationBuilder);
    }

    private List<TransactionView> mapToTransactionViewList(List<Transaction> transactionList, int statusVersion) {
        return transactionList.stream()
                .map(transaction -> TransactionView.from(transaction, statusVersion))
                .collect(Collectors.toList());
    }
    // @TODO(sfount) handling writing invalid transaction should be tested at `EventMessageHandler` integration level

    public void upsertTransactionFor(EventDigest eventDigest) {
        TransactionEntity transaction = transactionEntityFactory.create(eventDigest);
        transactionDao.upsert(transaction);
    }

    public TransactionEventResponse findTransactionEvents(String externalId, String gatewayAccountId,
                                                          boolean includeAllEvents, int statusVersion) {
        Map<String, TransactionEntity> transactionEntityMap = getTransactionsAsMap(externalId, gatewayAccountId);

        if (transactionEntityMap.isEmpty()) {
            throw new WebApplicationException(format("Transaction with id [%s] not found", externalId), Response.Status.NOT_FOUND);
        }

        List<TransactionEvent> transactionEvents = getTransactionEventsFor(transactionEntityMap, statusVersion);

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

    private List<TransactionEvent> getTransactionEventsFor(Map<String, TransactionEntity> transactionEntityMap, int statusVersion) {
        List<Event> events = eventDao.findEventsForExternalIds(transactionEntityMap.keySet());
        return mapToTransactionEvent(transactionEntityMap, events, statusVersion);
    }

    private List<TransactionEvent> mapToTransactionEvent(Map<String, TransactionEntity> transactionEntityMap, List<Event> eventList, int statusVersion) {
        return eventList.stream()
                .map(event -> TransactionEvent.from(transactionEntityMap.get(event.getResourceExternalId()), event, objectMapper, statusVersion))
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

    private TransactionsForTransactionResponse findTransactionsForParentExternalId(String parentTransactionExternalId, String gatewayAccountId) {
        List<TransactionView> transactions = transactionDao.findTransactionByParentIdAndGatewayAccountId(
                parentTransactionExternalId, gatewayAccountId)
                .stream()
                .sorted(Comparator.comparing(TransactionEntity::getCreatedDate))
                .map(transactionEntity ->
                        TransactionView.from(transactionFactory.createTransactionEntity(transactionEntity), DEFAULT_STATUS_VERSION))
                .collect(Collectors.toList());
        return TransactionsForTransactionResponse.of(parentTransactionExternalId, transactions);
    }
}
