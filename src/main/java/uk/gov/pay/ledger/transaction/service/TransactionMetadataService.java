package uk.gov.pay.ledger.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.metadatakey.dao.MetadataKeyDao;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transactionmetadata.dao.TransactionMetadataDao;

import java.util.Iterator;

public class TransactionMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionMetadataService.class);
    private final TransactionDao transactionDao;
    private final MetadataKeyDao metadataKeyDao;
    private final TransactionMetadataDao transactionMetadataDao;

    @Inject
    public TransactionMetadataService(MetadataKeyDao metadataKeyDao,
                                      TransactionMetadataDao transactionMetadataDao,
                                      TransactionDao transactionDao) {
        this.metadataKeyDao = metadataKeyDao;
        this.transactionMetadataDao = transactionMetadataDao;
        this.transactionDao = transactionDao;
    }

    public void upsertMetadataFor(Event event) {
        JsonNode eventDataNode;
        try {
            eventDataNode = new ObjectMapper().readTree(event.getEventData());
        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to parse incoming event payload: {}", e.getMessage());
            return;
        }
        if (eventDataNode != null && eventDataNode.has("external_metadata")) {
            transactionDao.findTransactionByExternalId(event.getResourceExternalId())
                    .ifPresent(transactionEntity -> {
                        Iterator<String> metadataKeys = eventDataNode.get("external_metadata").fieldNames();
                        metadataKeys.forEachRemaining(metadataKey -> {
                            metadataKeyDao.insertIfNotExist(metadataKey);
                            transactionMetadataDao
                                    .insertIfNotExist(transactionEntity.getId(), metadataKey);
                        });
                    });
        }
    }
}
