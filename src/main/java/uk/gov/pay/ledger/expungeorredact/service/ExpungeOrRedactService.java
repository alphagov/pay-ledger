package uk.gov.pay.ledger.expungeorredact.service;

import io.prometheus.client.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.ExpungeOrRedactHistoricalDataConfig;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.expungeorredact.dao.TransactionRedactionInfoDao;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;

import javax.inject.Inject;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.min;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Optional.ofNullable;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.service.payments.logging.LoggingKeys.RESOURCE_EXTERNAL_ID;

public class ExpungeOrRedactService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpungeOrRedactService.class);
    private final TransactionRedactionInfoDao transactionRedactionInfoDao;
    private final ExpungeOrRedactHistoricalDataConfig expungeOrRedactHistoricalDataConfig;
    private final Clock clock;
    private final TransactionDao transactionDao;
    private final EventDao eventDao;

    private static final int PAGE_SIZE = 500;

    private static final Histogram duration = Histogram.build()
            .name("expunge_and_redact_historical_data_job_duration_seconds")
            .help("Duration of expunge and redact historical data job in seconds")
            .unit("seconds")
            .register();

    private static final Histogram noOfTransactionsRedactedMetric = Histogram.build()
            .name("expunge_and_redact_historical_data_job_no_of_transactions_redacted")
            .help("Number of transactions redacted")
            .register();

    private static final Histogram noOfEventsDeletedRedactedMetric = Histogram.build()
            .name("expunge_and_redact_historical_data_job_no_of_transaction_events_deleted")
            .help("Number of transaction events deleted")
            .register();

    @Inject
    public ExpungeOrRedactService(TransactionDao transactionDao, EventDao eventDao,
                                  TransactionRedactionInfoDao transactionRedactionInfoDao,
                                  LedgerConfig ledgerConfig,
                                  Clock clock) {
        this.transactionDao = transactionDao;
        this.eventDao = eventDao;
        this.transactionRedactionInfoDao = transactionRedactionInfoDao;
        this.expungeOrRedactHistoricalDataConfig = ledgerConfig.getExpungeOrRedactHistoricalDataConfig();
        this.clock = clock;
    }

    public void redactOrDeleteData() {
        Histogram.Timer responseTimeTimer = duration.startTimer();
        try {
            if (expungeOrRedactHistoricalDataConfig.isExpungeAndRedactHistoricalDataEnabled()) {
                redactPIIFromTransactionsAndDeleteRelatedEvents();
            } else {
                LOGGER.info("Expunging and redacting historical data is not enabled");
            }
        } finally {
            responseTimeTimer.observeDuration();
        }
    }

    private void redactPIIFromTransactionsAndDeleteRelatedEvents() {
        int totalNoOfTransactionsToRedact = expungeOrRedactHistoricalDataConfig.getNoOfTransactionsToRedact();
        ZonedDateTime createdDateOfLastProcessedTransaction = getCreatedDateOfLastProcessedTransaction();
        ZonedDateTime redactTransactionsUpToDate = getRedactTransactionsUpToDate();

        int noOfTxsProcessed = 0;
        int noOfEventsDeleted = 0;

        while (noOfTxsProcessed < totalNoOfTransactionsToRedact) {
            int remainingNoOfTransactions = totalNoOfTransactionsToRedact - noOfTxsProcessed;
            int noOfTxsToFetch = min(remainingNoOfTransactions, PAGE_SIZE);

            List<TransactionEntity> transactionsForRedaction = transactionDao.findTransactionsForRedaction(
                    createdDateOfLastProcessedTransaction, redactTransactionsUpToDate, noOfTxsToFetch);

            if (transactionsForRedaction.isEmpty()) {
                break;
            }

            transactionsForRedaction.forEach(transactionEntity -> {
                transactionDao.redactPIIFromTransaction(transactionEntity.getExternalId());
                LOGGER.info("Redacted PII from transaction", kv(RESOURCE_EXTERNAL_ID, transactionEntity.getExternalId()));
            });

            List<String> transactionExternalIds = transactionsForRedaction
                    .stream()
                    .map(TransactionEntity::getExternalId)
                    .collect(Collectors.toList());

            noOfEventsDeleted += eventDao.deleteEventsForTransactions(transactionExternalIds);
            noOfTxsProcessed += transactionsForRedaction.size();

            createdDateOfLastProcessedTransaction = transactionsForRedaction
                    .stream().map(TransactionEntity::getCreatedDate)
                    .max(ZonedDateTime::compareTo).get();
            transactionRedactionInfoDao.update(createdDateOfLastProcessedTransaction);
        }

        LOGGER.info("Completed redacting PII from transactions",
                kv("no_of_transactions_redacted", noOfTxsProcessed),
                kv("no_of_events_deleted", noOfEventsDeleted));

        noOfTransactionsRedactedMetric.observe(noOfTxsProcessed);
        noOfEventsDeletedRedactedMetric.observe(noOfEventsDeleted);
    }

    private ZonedDateTime getRedactTransactionsUpToDate() {
        return clock.instant()
                .minus(expungeOrRedactHistoricalDataConfig.getExpungeOrRedactDataOlderThanDays(), DAYS)
                .atZone(UTC);
    }

    private ZonedDateTime getCreatedDateOfLastProcessedTransaction() {
        return ofNullable(transactionRedactionInfoDao.getCreatedDateOfLastProcessedTransaction())
                .orElseGet(this::getDayBeforeCreatedDateOfFirstTransaction)
                .withZoneSameInstant(UTC);
    }

    private ZonedDateTime getDayBeforeCreatedDateOfFirstTransaction() {
        ZonedDateTime dateOfFirstTransaction = transactionDao.getCreatedDateOfFirstTransaction()
                .orElseGet(this::getRedactTransactionsUpToDate)
                .minus(1, DAYS);

        transactionRedactionInfoDao.insert(dateOfFirstTransaction);

        return dateOfFirstTransaction;
    }
}
