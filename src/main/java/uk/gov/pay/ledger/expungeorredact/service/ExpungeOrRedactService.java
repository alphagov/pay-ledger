package uk.gov.pay.ledger.expungeorredact.service;

import io.prometheus.client.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.ExpungeOrRedactHistoricalDataConfig;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;

import javax.inject.Inject;

public class ExpungeOrRedactService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpungeOrRedactService.class);
    private final ExpungeOrRedactHistoricalDataConfig expungeOrRedactHistoricalDataConfig;
    private TransactionDao transactionDao;
    private EventDao eventDao;

    private static final Histogram duration = Histogram.build()
            .name("expunge_and_redact_historical_data_job_duration_seconds")
            .help("Duration of expunge and redact historical data job in seconds")
            .register();

    @Inject
    public ExpungeOrRedactService(TransactionDao transactionDao, EventDao eventDao, LedgerConfig ledgerConfig) {
        this.transactionDao = transactionDao;
        this.eventDao = eventDao;
        this.expungeOrRedactHistoricalDataConfig = ledgerConfig.getExpungeOrRedactHistoricalDataConfig();
    }

    public void redactOrDeleteData() {
        Histogram.Timer responseTimeTimer = duration.startTimer();
        try {
            if (!expungeOrRedactHistoricalDataConfig.isExpungeAndRedactHistoricalDataEnabled()) {
                LOGGER.info("Expunging and redacting historical data is not enabled");
            }
        } finally {
            responseTimeTimer.observeDuration();
        }
    }

}
