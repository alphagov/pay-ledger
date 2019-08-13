package uk.gov.pay.ledger.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
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

import javax.ws.rs.BadRequestException;
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

    public Optional<TransactionView> getTransactionForGatewayAccount(String gatewayAccountId, String transactionExternalId, TransactionType transactionType, String parentTransactionExternalId) {
        return transactionDao.findTransaction(transactionExternalId, gatewayAccountId, transactionType, parentTransactionExternalId)
                .map(entity -> TransactionView.from(transactionFactory.createTransactionEntity(entity)));
    }

    public Optional<TransactionView> getTransaction(String transactionExternalId) {
        return transactionDao.findTransactionByExternalId(transactionExternalId)
                .map(entity -> TransactionView.from(transactionFactory.createTransactionEntity(entity)));
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
        List<Transaction> transactionList = transactionDao.searchTransactions(searchParams)
                .stream()
                .map(transactionFactory::createTransactionEntity)
                .collect(Collectors.toList());
        Long total = transactionDao.getTotalForSearch(searchParams);
        PaginationBuilder paginationBuilder = new PaginationBuilder(searchParams, uriInfo);
        paginationBuilder = paginationBuilder.withTotalCount(total).buildResponse();

        List<TransactionView> transactionViewList = mapToTransactionViewList(transactionList);

        return new TransactionSearchResponse(searchParams.getAccountId(),
                total,
                (long) transactionList.size(),
                searchParams.getPageNumber(),
                transactionViewList
        ).withPaginationBuilder(paginationBuilder);
    }

    private List<TransactionView> mapToTransactionViewList(List<Transaction> transactionList) {
        return transactionList.stream()
                .map(transaction -> TransactionView.from(transaction))
                .collect(Collectors.toList());
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
                .map(event -> TransactionEvent.from(transactionEntityMap.get(event.getResourceExternalId()), event, objectMapper))
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
                        TransactionView.from(transactionFactory.createTransactionEntity(transactionEntity)))
                .collect(Collectors.toList());
        return TransactionsForTransactionResponse.of(parentTransactionExternalId, transactions);
    }
}
