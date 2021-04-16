package uk.gov.pay.ledger.transactionsummary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.transactionsummary.dao.TransactionSummaryDao;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import java.time.ZonedDateTime;

import static uk.gov.pay.ledger.transaction.state.TransactionState.CREATED;

public class TransactionSummaryDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();
    private TransactionSummaryDao transactionSummaryDao;
    private DatabaseTestHelper dbHelper;
    private TransactionFixture transactionFixture;

    @BeforeEach
    public void setUp() {
        transactionSummaryDao = new TransactionSummaryDao(rule.getJdbi());
    }

    @Test
    public void shouldUpsert() {
        transactionSummaryDao.upsert("1", ZonedDateTime.now(), CREATED, false, 1000L);
        transactionSummaryDao.upsert("1", ZonedDateTime.now(), CREATED, false, 1000L);
        transactionSummaryDao.upsert("1", ZonedDateTime.now(), CREATED, false, 1000L);
        System.out.println("");
    }
}