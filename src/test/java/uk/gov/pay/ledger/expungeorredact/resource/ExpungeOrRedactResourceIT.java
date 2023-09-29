package uk.gov.pay.ledger.expungeorredact.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.expungeorredact.dao.TransactionRedactionInfoDao;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.EventFixture.anEventFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

class ExpungeOrRedactResourceIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private Integer port = rule.getAppRule().getLocalPort();

    private DatabaseTestHelper databaseTestHelper;

    private TransactionRedactionInfoDao transactionRedactionInfoDao;

    @BeforeEach
    public void setUp() {
        transactionRedactionInfoDao = rule.getJdbi().onDemand(TransactionRedactionInfoDao.class);
        databaseTestHelper = aDatabaseTestHelper(rule.getJdbi());
        databaseTestHelper.truncateAllData();
    }

    public ExpungeOrRedactResourceIT() {
        super();
    }

    @Test
    void shouldRedactTransactionsCorrectlyOnFirstAndSubsequentInvokes() {
        TransactionEntity txToRedactOnFirstRun1 = createTransactionAndEvent("should-redact-on-first-run",
                Instant.now().minus(30, DAYS).atZone(UTC));
        TransactionEntity txToRedactOnFirstRun2 = createTransactionAndEvent("should-redact-on-first-run",
                Instant.now().minus(20, DAYS).atZone(UTC));
        TransactionEntity txToRedactOnSubsequentRun1 = createTransactionAndEvent("should-redact-on-second-run",
                Instant.now().minus(11, DAYS).atZone(UTC));
        TransactionEntity txThatShouldNotBeProcessed = createTransactionAndEvent("should-not-redact-not-older-than-2-days",
                Instant.now().atZone(UTC));

        given().port(port)
                .contentType(JSON)
                .post("/v1/tasks/expunge-or-redact-historical-data")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        assertPIIRedactedForTransactionsAndRelatedEventsDeleted(txToRedactOnFirstRun1, txToRedactOnFirstRun2);
        assertPIINotRedactedForTransactions(txToRedactOnSubsequentRun1, txThatShouldNotBeProcessed);

        assertThat(transactionRedactionInfoDao.getCreatedDateOfLastProcessedTransaction().withZoneSameInstant(UTC),
                is(txToRedactOnFirstRun2.getCreatedDate().withZoneSameInstant(UTC)));

        given().port(port)
                .contentType(JSON)
                .post("/v1/tasks/expunge-or-redact-historical-data")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        assertPIIRedactedForTransactionsAndRelatedEventsDeleted(txToRedactOnSubsequentRun1);
        assertPIINotRedactedForTransactions(txThatShouldNotBeProcessed);

        assertThat(transactionRedactionInfoDao.getCreatedDateOfLastProcessedTransaction().withZoneSameInstant(UTC),
                is(txToRedactOnSubsequentRun1.getCreatedDate().withZoneSameInstant(UTC)));
    }

    @Test
    void shouldNotRedactTransactionsNotOlderThanTheConfiguredDays() {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withCreatedDate(Instant.now().atZone(UTC))
                .withReference("ref-1")
                .withCardholderName("Jane D")
                .insert(rule.getJdbi())
                .toEntity();

        given().port(port)
                .contentType(JSON)
                .post("/v1/tasks/expunge-or-redact-historical-data")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        List<Map<String, Object>> allTransactions = databaseTestHelper.getAllTransactions();

        assertThat(allTransactions.get(0).get("reference"), is(transactionEntity.getReference()));
        assertThat(allTransactions.get(0).get("cardholder_name"), is(transactionEntity.getCardholderName()));
    }

    private TransactionEntity createTransactionAndEvent(String reference, ZonedDateTime createdDate) {
        TransactionEntity transactionEntity = aTransactionFixture()
                .withReference(reference)
                .withCreatedDate(createdDate)
                .insert(rule.getJdbi())
                .toEntity();

        anEventFixture().withResourceExternalId(transactionEntity.getExternalId()).insert(rule.getJdbi());

        return transactionEntity;
    }

    private void assertPIINotRedactedForTransactions(TransactionEntity... transactionEntities) {
        Arrays.stream(transactionEntities)
                .forEach(transactionEntity -> {
                    Map<String, Object> transaction = databaseTestHelper.getTransaction(transactionEntity.getExternalId());
                    assertThat(transaction.get("reference"), is(transactionEntity.getReference()));
                    assertThat(transaction.get("cardholder_name"), is(transactionEntity.getCardholderName()));
                    assertThat(transaction.get("email"), is(transactionEntity.getEmail()));

                    Map<String, Object> event = databaseTestHelper.getEventByExternalId(transactionEntity.getExternalId());
                    assertThat(event.size(), greaterThan(0));
                });
    }

    private void assertPIIRedactedForTransactionsAndRelatedEventsDeleted(TransactionEntity... transactionEntities) {
        Arrays.stream(transactionEntities)
                .forEach(transactionEntity -> {
                    Map<String, Object> transaction = databaseTestHelper.getTransaction(transactionEntity.getExternalId());
                    assertThat(transaction.get("reference"), is("<DELETED>"));
                    assertThat(transaction.get("cardholder_name"), is("<DELETED>"));
                    assertThat(transaction.get("email"), is("<DELETED>"));
                    assertThat(transaction.get("description"), is("<DELETED>"));

                    List<Map<String, Object>> event = databaseTestHelper.getEventsByExternalId(transactionEntity.getExternalId());
                    assertThat(event.size(), is(0));
                });
    }
}
