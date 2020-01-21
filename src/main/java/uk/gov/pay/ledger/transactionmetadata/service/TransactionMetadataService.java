package uk.gov.pay.ledger.transactionmetadata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.metadata.dao.MetadataDao;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transactionmetadata.dao.TransactionMetadataDao;

import java.util.Iterator;

public class TransactionMetadataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionMetadataService.class);

    private TransactionMetadataDao transactionMetadataDao;
    private MetadataDao metadataDao;
    private TransactionDao transactionDao;

    @Inject
    public void TransactionMetadataService(TransactionDao transactionDao,
                                           TransactionMetadataDao transactionMetadataDao,
                                           MetadataDao metadataDao) {
        this.transactionDao = transactionDao;
        this.transactionMetadataDao = transactionMetadataDao;
        this.metadataDao = metadataDao;
    }

    public void processTransactionMetadata(String resourceExternalId, JsonNode externalMetadata) {
        transactionDao.findTransactionByExternalId(resourceExternalId).ifPresent(transactionEntity -> {

            Iterator<String> fieldNamesIterator = externalMetadata.fieldNames();
            while (fieldNamesIterator.hasNext()) {
                String metadataKey = fieldNamesIterator.next();

                metadataDao.insertIfNotExist(metadataKey);
                transactionMetadataDao.insertIfNotExist(transactionEntity.getId(), metadataKey);

                LOGGER.info("inserted transaction metadata for key [{}]", metadataKey);
            }
        });
    }
}
