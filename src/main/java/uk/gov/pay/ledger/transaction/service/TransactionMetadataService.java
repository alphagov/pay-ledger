package uk.gov.pay.ledger.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.dao.entity.EventEntity;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.gatewayaccountmetadata.service.GatewayAccountMetadataService;
import uk.gov.pay.ledger.metadatakey.dao.MetadataKeyDao;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transactionmetadata.dao.TransactionMetadataDao;

import java.util.Map;
import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.kv;

public class TransactionMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionMetadataService.class);
    private final TransactionDao transactionDao;
    private final MetadataKeyDao metadataKeyDao;
    private final TransactionMetadataDao transactionMetadataDao;
    private final GatewayAccountMetadataService gatewayAccountMetadataService;


    @Inject
    public TransactionMetadataService(MetadataKeyDao metadataKeyDao,
                                      TransactionMetadataDao transactionMetadataDao,
                                      TransactionDao transactionDao,
                                      GatewayAccountMetadataService gatewayAccountMetadataService) {
        this.metadataKeyDao = metadataKeyDao;
        this.transactionMetadataDao = transactionMetadataDao;
        this.transactionDao = transactionDao;
        this.gatewayAccountMetadataService = gatewayAccountMetadataService;

    }

    public void upsertMetadataFor(EventEntity event) {
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
                        eventDataNode.get("external_metadata").fields().forEachRemaining(metadata -> {
                            try {
                                metadataKeyDao.insertIfNotExist(metadata.getKey());
                            } catch (UnableToExecuteStatementException ex) {
                                LOGGER.info("Metadata key already exists",
                                        kv("key", metadata.getKey()),
                                        kv("exception", ex.getMessage()));
                            }
                            String value = metadata.getValue().asText();
                            transactionMetadataDao
                                    .upsert(transactionEntity.getId(), metadata.getKey(), value);
                            gatewayAccountMetadataService.upsertMetadataKeyForGatewayAccount(transactionEntity.getGatewayAccountId(), metadata.getKey());
                        });
                    });
        }
    }

    public void reprojectFromEventDigest(EventDigest eventDigest) {
        var eventData = eventDigest.getEventAggregate();

        transactionDao.findTransactionByExternalId(eventDigest.getResourceExternalId())
                .ifPresent(transactionEntity -> {
                    Optional.ofNullable(eventData.get("external_metadata"))
                            .ifPresent(metadataObj -> {
                                @SuppressWarnings("unchecked")
                                var metadata = (Map<String, Object>) metadataObj;
                                metadata.forEach((key, value) -> {
                                    metadataKeyDao.insertIfNotExist(key);
                                    transactionMetadataDao.upsert(transactionEntity.getId(), key, value.toString());
                                    gatewayAccountMetadataService.upsertMetadataKeyForGatewayAccount(transactionEntity.getGatewayAccountId(), key);
                                });
                            });
                });
    }
}
