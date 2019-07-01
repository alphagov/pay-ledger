package uk.gov.pay.ledger.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.Payment;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.search.common.HalLinkBuilder;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.model.ConvertedTransactionDetails;
import uk.gov.pay.ledger.transaction.search.model.Link;
import uk.gov.pay.ledger.transaction.search.model.PaginationBuilder;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TransactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionService.class);
    private final TransactionDao transactionDao;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public TransactionService(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    public Optional<TransactionView> getTransaction(String transactionExternalId, UriInfo uriInfo) {
            return transactionDao.findTransactionByExternalId(transactionExternalId)
                    .map(entity -> decorateWithLinks(TransactionView.from(Payment.fromTransactionEntity(entity)), uriInfo));
    }

    public TransactionSearchResponse searchTransactions(TransactionSearchParams searchParams, UriInfo uriInfo) {
        List<Payment> transactionList = transactionDao.searchTransactions(searchParams)
                .stream()
                .map(Payment::fromTransactionEntity)
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

    public void upsertTransactionFor(EventDigest eventDigest) {
        TransactionEntity transaction = convertToTransaction(eventDigest);
        transactionDao.upsert(transaction);
    }

    public TransactionEntity convertToTransaction(EventDigest eventDigest) {
        String transactionDetail = convertToTransactionDetails(eventDigest.getEventPayload());
        TransactionEntity entity = objectMapper.convertValue(eventDigest.getEventPayload(), TransactionEntity.class);
        entity.setTransactionDetails(transactionDetail);
        entity.setEventCount(eventDigest.getEventCount());
        entity.setState(TransactionState.fromSalientEventType(eventDigest.getMostRecentSalientEventType()).getState());
        entity.setCreatedDate(eventDigest.getEventCreatedDate());
        entity.setExternalId(eventDigest.getResourceExternalId());

        return entity;
    }

    private String convertToTransactionDetails(Map<String, Object> transactionPayload) {
        ConvertedTransactionDetails details = objectMapper.convertValue(transactionPayload, ConvertedTransactionDetails.class);
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to parse incoming event payload: {}", e.getMessage());
        }
        return "{}";
    }
}
