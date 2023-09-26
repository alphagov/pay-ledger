package uk.gov.pay.ledger.expungeorredact.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.ExpungeOrRedactHistoricalDataConfig;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;

import java.util.List;
import java.util.Optional;

import static ch.qos.logback.classic.Level.INFO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpungeOrRedactServiceTest {

    @Mock
    TransactionDao mockTransactionDao;
    @Mock
    EventDao mockEventDao;

    @Mock
    LedgerConfig mockLedgerConfig;

    @Mock
    ExpungeOrRedactHistoricalDataConfig mockExpungeOrRedactHistoricalDataConfig;
    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;

    private final CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;

    ExpungeOrRedactService expungeOrRedactService;

    @BeforeEach
    void setUp() {
        when(mockLedgerConfig.getExpungeOrRedactHistoricalDataConfig()).thenReturn(mockExpungeOrRedactHistoricalDataConfig);
        expungeOrRedactService = new ExpungeOrRedactService(mockTransactionDao, mockEventDao, mockLedgerConfig);
    }

    @Test
    void shouldLogIfExpungingAndRedactingDataIsNotEnabled() {
        Logger root = (Logger) LoggerFactory.getLogger(ExpungeOrRedactService.class);
        root.addAppender(mockAppender);
        root.setLevel(INFO);

        when(mockExpungeOrRedactHistoricalDataConfig.isExpungeAndRedactHistoricalDataEnabled()).thenReturn(false);
        expungeOrRedactService.redactOrDeleteData();

        verify(mockAppender, times(1)).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(loggingEvents.get(0).getFormattedMessage(), is("Expunging and redacting historical data is not enabled"));

        verifyNoInteractions(mockTransactionDao);
        verifyNoInteractions(mockEventDao);
    }

    @Test
    void shouldObserveJobDurationForMetrics() {
        Double initialDuration = Optional.ofNullable(collectorRegistry.getSampleValue("expunge_and_redact_historical_data_job_duration_seconds_sum")).orElse(0.0);

        expungeOrRedactService.redactOrDeleteData();

        Double duration = collectorRegistry.getSampleValue("expunge_and_redact_historical_data_job_duration_seconds_sum");
        assertThat(duration, greaterThan(initialDuration));
    }
}
