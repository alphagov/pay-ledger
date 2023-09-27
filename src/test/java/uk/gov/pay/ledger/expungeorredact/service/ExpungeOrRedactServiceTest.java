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
import uk.gov.pay.ledger.expungeorredact.dao.TransactionRedactionInfoDao;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static ch.qos.logback.classic.Level.INFO;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.parse;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

@ExtendWith(MockitoExtension.class)
class ExpungeOrRedactServiceTest {

    @Mock
    TransactionDao mockTransactionDao;
    @Mock
    EventDao mockEventDao;

    @Mock
    TransactionRedactionInfoDao mockTransactionRedactionInfoDao;

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

    String SYSTEM_INSTANT = "2022-03-03T10:15:30Z";
    Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse(SYSTEM_INSTANT), UTC);
        when(mockLedgerConfig.getExpungeOrRedactHistoricalDataConfig()).thenReturn(mockExpungeOrRedactHistoricalDataConfig);
        expungeOrRedactService = new ExpungeOrRedactService(mockTransactionDao, mockEventDao, mockTransactionRedactionInfoDao, mockLedgerConfig, clock);
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

    @Test
    void shouldNotRedactPIIFromTransactionsWhenNoTransactionsFound() {
        when(mockExpungeOrRedactHistoricalDataConfig.isExpungeAndRedactHistoricalDataEnabled()).thenReturn(true);
        when(mockExpungeOrRedactHistoricalDataConfig.getNoOfTransactionsToRedact()).thenReturn(100);
        when(mockExpungeOrRedactHistoricalDataConfig.getExpungeOrRedactDataOlderThanDays()).thenReturn(1);

        when(mockTransactionRedactionInfoDao.getCreatedDateOfLastProcessedTransaction()).thenReturn(parse("2020-01-01T10:15:30Z"));
        when(mockTransactionDao.findTransactionsForRedaction(any(), any(), anyInt())).thenReturn(List.of());

        expungeOrRedactService.redactOrDeleteData();

        verify(mockTransactionDao).findTransactionsForRedaction(
                parse("2020-01-01T10:15:30Z"),
                parse(SYSTEM_INSTANT).minus(1, DAYS),
                100);

        verify(mockTransactionDao, never()).redactPIIFromTransaction(any());
        verify(mockTransactionRedactionInfoDao).getCreatedDateOfLastProcessedTransaction();

        verifyNoMoreInteractions(mockTransactionDao);
        verifyNoMoreInteractions(mockTransactionRedactionInfoDao);
        verifyNoInteractions(mockEventDao);
    }

    @Test
    void shouldRedactPIIFromTransactionsAndDeleteEvents() {
        when(mockExpungeOrRedactHistoricalDataConfig.isExpungeAndRedactHistoricalDataEnabled()).thenReturn(true);
        when(mockExpungeOrRedactHistoricalDataConfig.getNoOfTransactionsToRedact()).thenReturn(100);
        when(mockExpungeOrRedactHistoricalDataConfig.getExpungeOrRedactDataOlderThanDays()).thenReturn(1);

        when(mockTransactionRedactionInfoDao.getCreatedDateOfLastProcessedTransaction()).thenReturn(parse("2020-01-01T10:15:30Z"));

        TransactionEntity transactionEntity1 = aTransactionFixture().toEntity();
        TransactionEntity transactionEntity2 = aTransactionFixture().toEntity();
        when(mockTransactionDao.findTransactionsForRedaction(any(), any(), anyInt())).thenReturn(
                List.of(transactionEntity1, transactionEntity2)
        );

        expungeOrRedactService.redactOrDeleteData();

        verify(mockTransactionDao).findTransactionsForRedaction(
                parse("2020-01-01T10:15:30Z"),
                parse(SYSTEM_INSTANT).minus(1, DAYS),
                100);

        verify(mockTransactionDao, times(2)).redactPIIFromTransaction(any());
        verify(mockEventDao).deleteEventsForTransactions(List.of(transactionEntity1.getExternalId(), transactionEntity2.getExternalId()));
        verify(mockTransactionRedactionInfoDao).update(transactionEntity2.getCreatedDate());

        verifyNoMoreInteractions(mockTransactionDao);
        verifyNoMoreInteractions(mockTransactionRedactionInfoDao);
    }

    @Test
    void redactOrDeleteDataShouldReturnWhenNoOfTransactionsFoundToRedactIsLessThanTheConfiguredNoOfTransactionsToRedact() {
        when(mockExpungeOrRedactHistoricalDataConfig.isExpungeAndRedactHistoricalDataEnabled()).thenReturn(true);
        when(mockExpungeOrRedactHistoricalDataConfig.getNoOfTransactionsToRedact()).thenReturn(999999);
        when(mockExpungeOrRedactHistoricalDataConfig.getExpungeOrRedactDataOlderThanDays()).thenReturn(1);

        TransactionEntity transactionEntity1 = aTransactionFixture().toEntity();
        TransactionEntity transactionEntity2 = aTransactionFixture().toEntity();
        when(mockTransactionDao.findTransactionsForRedaction(any(), any(), anyInt())).thenReturn(
                List.of(transactionEntity1, transactionEntity2),
                List.of() // returns empty list for subsequent calls
        );

        when(mockTransactionRedactionInfoDao.getCreatedDateOfLastProcessedTransaction()).thenReturn(parse("2020-01-01T10:15:30Z"));

        expungeOrRedactService.redactOrDeleteData();

        verify(mockTransactionDao).findTransactionsForRedaction(
                parse("2020-01-01T10:15:30Z"),
                parse(SYSTEM_INSTANT).minus(1, DAYS),
                500);

        verify(mockTransactionDao, times(2)).redactPIIFromTransaction(any());
        verify(mockEventDao).deleteEventsForTransactions(List.of(transactionEntity1.getExternalId(), transactionEntity2.getExternalId()));
        verify(mockTransactionRedactionInfoDao).update(transactionEntity2.getCreatedDate());

        verifyNoMoreInteractions(mockTransactionDao);
        verifyNoMoreInteractions(mockTransactionRedactionInfoDao);
    }

    @Test
    void shouldUseCreatedDateOfFirstTransactionWhenInfoIsNotAvailableInTransactionRedactionInfo() {
        when(mockExpungeOrRedactHistoricalDataConfig.isExpungeAndRedactHistoricalDataEnabled()).thenReturn(true);
        when(mockExpungeOrRedactHistoricalDataConfig.getNoOfTransactionsToRedact()).thenReturn(100);
        when(mockExpungeOrRedactHistoricalDataConfig.getExpungeOrRedactDataOlderThanDays()).thenReturn(1);

        when(mockTransactionRedactionInfoDao.getCreatedDateOfLastProcessedTransaction()).thenReturn(null);
        when(mockTransactionDao.getCreatedDateOfFirstTransaction()).thenReturn(Optional.of(parse("2020-01-01T10:15:30Z")));

        expungeOrRedactService.redactOrDeleteData();

        verify(mockTransactionDao).getCreatedDateOfFirstTransaction();
        verify(mockTransactionRedactionInfoDao).insert(parse("2020-01-01T10:15:30Z"));
    }

    @Test
    void shouldUseTheDateDerivedBasedOnConfigForTheStartingIndexWhenNoTransactionsExists() {
        when(mockExpungeOrRedactHistoricalDataConfig.isExpungeAndRedactHistoricalDataEnabled()).thenReturn(true);
        when(mockExpungeOrRedactHistoricalDataConfig.getNoOfTransactionsToRedact()).thenReturn(100);
        when(mockExpungeOrRedactHistoricalDataConfig.getExpungeOrRedactDataOlderThanDays()).thenReturn(1);

        when(mockTransactionRedactionInfoDao.getCreatedDateOfLastProcessedTransaction()).thenReturn(null);
        when(mockTransactionDao.getCreatedDateOfFirstTransaction()).thenReturn(Optional.ofNullable(null));

        expungeOrRedactService.redactOrDeleteData();

        verify(mockTransactionDao).getCreatedDateOfFirstTransaction();
        verify(mockTransactionRedactionInfoDao).insert(parse("2022-03-02T10:15:30Z"));
    }

}
