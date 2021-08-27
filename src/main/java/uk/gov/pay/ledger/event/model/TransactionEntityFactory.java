package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.util.Map;

public class TransactionEntityFactory {

    private ObjectMapper objectMapper;
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionEntityFactory.class);

    @Inject
    public TransactionEntityFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TransactionEntity create(EventDigest eventDigest) {
        return create(eventDigest, eventDigest.getEventPayload());
    }

    public TransactionEntity create(EventDigest eventDigest, Map<String, Object> eventPayload) {
        TransactionState digestTransactionState = eventDigest
                .getMostRecentSalientEventType()
                .map(TransactionState::fromEventType)
                .orElse(TransactionState.UNDEFINED);

        String transactionDetail = convertToTransactionDetails(eventPayload);
        TransactionEntity entity = objectMapper.convertValue(eventPayload, TransactionEntity.class);
        entity.setTransactionDetails(transactionDetail);
        entity.setEventCount(eventDigest.getEventCount());
        entity.setState(digestTransactionState);
        entity.setCreatedDate(eventDigest.getEventCreatedDate());
        entity.setExternalId(eventDigest.getResourceExternalId());
        entity.setParentExternalId(eventDigest.getParentResourceExternalId());
        entity.setTransactionType(eventDigest.getResourceType().toString());

        return entity;
    }

    private String convertToTransactionDetails(Map<String, Object> transactionPayload) {
        try {
            return objectMapper.writeValueAsString(transactionPayload);
        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to parse incoming event payload: {}", e.getMessage());
        }
        return "{}";
    }

}
