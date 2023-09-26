package uk.gov.pay.ledger.expungeorredact.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.util.DatabaseTestHelper;

import java.time.Instant;
import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;

class TransactionRedactionInfoDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private TransactionRedactionInfoDao transactionRedactionInfoDao;

    private DatabaseTestHelper databaseTestHelper = aDatabaseTestHelper(rule.getJdbi());

    @BeforeEach
    public void setUp() {
        transactionRedactionInfoDao = rule.getJdbi().onDemand(TransactionRedactionInfoDao.class);
        databaseTestHelper.truncateAllData();
    }

    @Test
    void shouldCreateAndReturnLastProcessedTransactionCreatedDate() {
        ZonedDateTime transactionDate = Instant.now().atZone(UTC);
        transactionRedactionInfoDao.insert(transactionDate);

        ZonedDateTime createdDateOfLastProcessedTransaction =
                transactionRedactionInfoDao.getCreatedDateOfLastProcessedTransaction().withZoneSameInstant(UTC);

        assertThat(createdDateOfLastProcessedTransaction, is(transactionDate));
    }

    @Test
    void shouldUpdateTransactionRedactionInfoCorrectly() {
        ZonedDateTime transactionDate = Instant.now().atZone(UTC);
        transactionRedactionInfoDao.insert(transactionDate);

        ZonedDateTime transactionDateToUpdate = Instant.now().plus(10, DAYS).atZone(UTC);
        transactionRedactionInfoDao.update(transactionDateToUpdate);

        ZonedDateTime createdDateOfLastProcessedTransaction =
                transactionRedactionInfoDao.getCreatedDateOfLastProcessedTransaction().withZoneSameInstant(UTC);
        assertThat(createdDateOfLastProcessedTransaction, is(transactionDateToUpdate));
    }

}
