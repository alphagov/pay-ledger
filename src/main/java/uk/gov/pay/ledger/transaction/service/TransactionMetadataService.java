package uk.gov.pay.ledger.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import jersey.repackaged.com.google.common.base.Stopwatch;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.metadatakey.dao.MetadataKeyDao;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transactionmetadata.dao.TransactionMetadataDao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static net.logstash.logback.argument.StructuredArguments.kv;

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
                                });
                            });
                });
    }

    List<String> findMetadataKeysForTransactions(TransactionSearchParams searchParams) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        List<String> metadataKeysForTransactions = transactionMetadataDao.findMetadataKeysForTransactions(searchParams);

        long elapsed = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
        LOGGER.info("CSV metadata headers calculated.",
                kv("time_taken_in_milli_seconds", elapsed),
                kv("number_of_metadata_keys", metadataKeysForTransactions.size()));

        return metadataKeysForTransactions;
    }
}
