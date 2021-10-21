package uk.gov.pay.ledger.transaction.dao;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.ReportingConfig;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.Transaction;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.CommaDelimitedSetParameter;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static java.time.ZonedDateTime.now;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.PayoutFixture.PayoutFixtureBuilder.aPayoutFixture;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aPersistedTransactionList;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionDaoSearchIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private TransactionFixture transactionFixture;
    private TransactionDao transactionDao;
    private TransactionSearchParams searchParams;

    private DatabaseTestHelper databaseTestHelper = aDatabaseTestHelper(rule.getJdbi());

    @Mock
    private LedgerConfig ledgerConfig;
    @Mock
    private ReportingConfig reportingConfig;

    @BeforeEach
    public void setUp() {
        when(reportingConfig.getSearchQueryTimeoutInSeconds()).thenReturn(50);
        when(ledgerConfig.getReportingConfig()).thenReturn(reportingConfig);
        databaseTestHelper.truncateAllData();
        transactionDao = new TransactionDao(rule.getJdbi(), ledgerConfig);
        searchParams = new TransactionSearchParams();
    }

    @Test
    public void shouldGetAndMapTransactionCorrectly() {

        transactionFixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withMoto(true)
                .insert(rule.getJdbi());

        searchParams.setAccountIds(List.of(transactionFixture.getGatewayAccountId()));

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(1));
        TransactionEntity transaction = transactionList.get(0);

        assertThat(transaction.getId(), is(transactionFixture.getId()));
        assertThat(transaction.getGatewayAccountId(), is(transactionFixture.getGatewayAccountId()));
        assertThat(transaction.getExternalId(), is(transactionFixture.getExternalId()));
        assertThat(transaction.getAmount(), is(transactionFixture.getAmount()));
        assertThat(transaction.getReference(), is(transactionFixture.getReference()));
        assertThat(transaction.getDescription(), is(transactionFixture.getDescription()));
        assertThat(transaction.getState(), is(transactionFixture.getState()));
        assertThat(transaction.getEmail(), is(transactionFixture.getEmail()));
        assertThat(transaction.getCardholderName(), is(transactionFixture.getCardDetails().getCardHolderName()));
        assertThat(transaction.getCardBrand(), is(transactionFixture.getCardDetails().getCardBrand()));
        assertThat(transaction.getCreatedDate(), is(transactionFixture.getCreatedDate()));
        assertThat(transaction.isMoto(), is(transactionFixture.isMoto()));

        Long total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(1L));
    }

    @Test
    public void shouldReturn2Records_whenSearchingBySpecificGatewayAccountId() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .insert(rule.getJdbi());
        }
        aTransactionFixture()
                .insert(rule.getJdbi());

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(2));
        assertThat(transactionList.get(0).getGatewayAccountId(), is(gatewayAccountId));
        assertThat(transactionList.get(1).getGatewayAccountId(), is(gatewayAccountId));

        Long total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(2L));
    }

    @Test
    public void shouldReturn1Record_whenSearchingByEmail() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withEmail("testemail" + i + "@example.org")
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setEmail("testemail1");

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), is(1));
        assertThat(transactionList.get(0).getEmail(), is("testemail1@example.org"));

        Long total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(1L));
    }

    @Test
    public void shouldReturn1Record_whenSearchingByExactReference() {

        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withAmount(100L + i)
                    .withGatewayAccountId(gatewayAccountId)
                    .withReference("reference " + i)
                    .withDescription("description " + i)
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setExactReferenceMatch(true);
        searchParams.setReference("reference 1");

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(1));
        assertThat(transactionList.get(0).getReference(), is("reference 1"));

        Long total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(1L));
    }

    @Test
    public void shouldReturn1Record_whenSearchingByPartialReference() {

        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withAmount(100L + i)
                    .withGatewayAccountId(gatewayAccountId)
                    .withReference("reference " + i)
                    .withDescription("description " + i)
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setExactReferenceMatch(false);
        searchParams.setReference("1");

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(1));
        assertThat(transactionList.get(0).getReference(), is("reference 1"));

        Long total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(1L));
    }

    @Test
    public void shouldReturn1Record_whenSearchingByCardHolderName() {

        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withReference("reference " + i)
                    .withCardholderName("name" + i)
                    .withDescription("description " + i)
                    .withCreatedDate(now().plusSeconds(1L))
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setCardHolderName("name1");

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(1));
        assertThat(transactionList.get(0).getCardholderName(), is("name1"));

        Long total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(1L));
    }

    @Test
    public void shouldReturn1Record_withFromDateSet() {

        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 1; i < 4; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withCreatedDate(now().minusDays(i))
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setFromDate(now().minusDays(1).minusMinutes(10).toString());

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(1));

        Long total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(1L));
    }

    @Test
    public void shouldReturn2Records_withToDateSet() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 1; i < 4; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withCreatedDate(now().minusDays(i))
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setToDate(now().minusDays(2).plusMinutes(10).toString());

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(2));

        Long total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(2L));
    }

    @Test
    public void shouldReturn10Records_withPagesize10() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 1; i < 20; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setDisplaySize(10L);

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(10));

        Long total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(19L));
    }

    @Test
    public void shouldReturn2Records_withOffsetAndPagesizeSet() {
        String gatewayAccountId = "account-id-" + nextLong();
        long id = nextLong();

        for (int i = 1; i < 20; i++) {
            aTransactionFixture()
                    .withId(id + i)
                    .withGatewayAccountId(gatewayAccountId)
                    .withReference("reference" + i)
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setDisplaySize(2L);
        searchParams.setPageNumber(3L);


        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(2));
        assertThat(transactionList.get(0).getReference(), is("reference15"));
        assertThat(transactionList.get(1).getReference(), is("reference14"));

        Long total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(19L));
    }

    @Test
    public void shouldReturn2Records_WhenSearchingByCreatedState() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withState(TransactionState.CREATED)
                    .withGatewayAccountId(gatewayAccountId)
                    .insert(rule.getJdbi());
        }
        aTransactionFixture()
                .withState(TransactionState.SUBMITTED)
                .withGatewayAccountId(gatewayAccountId)
                .insert(rule.getJdbi());


        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setState("created");

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(2));

        Long total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(2L));
    }

    @Test
    public void shouldReturn2Records_whenLastDigitsCardNumberIsSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withLastDigitsCardNumber("1234")
                    .insert(rule.getJdbi());
        }
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setLastDigitsCardNumber("1234");

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(2));
    }

    @Test
    public void shouldReturn2Records_whenFirstDigitsCardNumberIsSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withFirstDigitsCardNumber("123456")
                    .insert(rule.getJdbi());
        }
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setFirstDigitsCardNumber("123456");

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(2));
    }

    @Test
    public void shouldReturn2Records_whenCardBrandsIsSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 2; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withCardBrand(i % 2 == 0 ? "visa" : "mastercard")
                    .insert(rule.getJdbi());
        }
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setCardBrands(new CommaDelimitedSetParameter("visa,mastercard"));
        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(2));
    }

    @Test
    public void shouldReturn2Records_whenSingleCardBrandIsSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 4; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withCardBrand(i % 2 == 0 ? "visa" : "mastercard")
                    .insert(rule.getJdbi());
        }
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setCardBrands(new CommaDelimitedSetParameter("visa"));
        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(2));
    }

    @Test
    public void shouldReturn2Records_whenCardholderNameIsSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 4; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withCardholderName(i % 2 == 0 ? "J Smith" : "P Howard")
                    .insert(rule.getJdbi());
        }
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setCardHolderName("smith");

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(2));
    }

    @Test
    public void shouldReturn2Records_whenTransactionTypeIsSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 4; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withTransactionType(i % 2 == 0 ? "REFUND" : "PAYMENT")
                    .insert(rule.getJdbi());
        }
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setTransactionType(TransactionType.REFUND);

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(2));
        assertTrue(transactionList.stream().allMatch(e -> "REFUND".equals(e.getTransactionType())));
    }

    @Test
    public void shouldReturn4Records_whenTransactionTypeIsNotSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 0; i < 4; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .withTransactionType(i % 2 == 0 ? "REFUND" : "PAYMENT")
                    .insert(rule.getJdbi());
        }
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(4));
    }

    @Test
    public void shouldFilterByGatewayAccountIdWhenSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();

        TransactionFixture transaction = aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType("PAYMENT")
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId + "different_account")
                .withTransactionType("PAYMENT")
                .insert(rule.getJdbi());
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(1));
        assertThat(transactionList.get(0).getExternalId(), is(transaction.getExternalId()));
    }

    @Test
    public void shouldFilterByGatewayTransactionIdWhenSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();
        String gatewayTransactionId = "transaction-id-" + nextLong();

        TransactionFixture transaction = aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayTransactionId(gatewayTransactionId)
                .withTransactionType("PAYMENT")
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayTransactionId(gatewayTransactionId + "different-id")
                .withTransactionType("PAYMENT")
                .insert(rule.getJdbi());
        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setGatewayTransactionId(gatewayTransactionId);

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(1));
        assertThat(transactionList.get(0).getExternalId(), is(transaction.getExternalId()));
        assertThat(transactionList.get(0).getGatewayTransactionId(), is(transaction.getGatewayTransactionId()));
    }

    @Test
    public void searchTransactionsByMetadataValue() {
        String gatewayAccountId = "account-id-" + nextLong();
        String reference = randomAlphanumeric(15);

        TransactionEntity transaction1 = insertTransaction(gatewayAccountId, reference, ZonedDateTime.now().minusDays(2),
                ImmutableMap.of("test-key-1", "value1", "test-key-2", "value1"));
        TransactionEntity transaction2 = insertTransaction(gatewayAccountId, reference, ZonedDateTime.now(),
                ImmutableMap.of("test-key-n", "VALUE1"));
        TransactionEntity transactionToBeExcluded = insertTransaction(gatewayAccountId, "ref123", ZonedDateTime.now(),
                ImmutableMap.of("test-key-3", "value3"));

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setReference(reference);
        searchParams.setMetadataValue("value1");

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);
        Long totalForSearch = transactionDao.getTotalForSearch(searchParams);

        assertThat(transactionList.size(), Matchers.is(2));
        assertThat(totalForSearch, Matchers.is(2L));

        assertThat(transactionList.get(0).getExternalId(), is(transaction2.getExternalId()));
        JsonElement externalMetadata = JsonParser.parseString(transactionList.get(0).getTransactionDetails()).getAsJsonObject().get("external_metadata");
        assertThat(externalMetadata.getAsJsonObject().get("test-key-n").getAsString(), is("VALUE1"));

        assertThat(transactionList.get(1).getExternalId(), is(transaction1.getExternalId()));
        externalMetadata = JsonParser.parseString(transactionList.get(1).getTransactionDetails()).getAsJsonObject().get("external_metadata");
        assertThat(externalMetadata.getAsJsonObject().get("test-key-1").getAsString(), is("value1"));
        assertThat(externalMetadata.getAsJsonObject().get("test-key-2").getAsString(), is("value1"));
    }

    @Test
    public void shouldFilterByGatewayPayoutIdWhenSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();
        String gatewayPayoutId = "payout-id-" + nextLong();

        TransactionFixture transaction = aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId)
                .withCreatedDate(now().minusDays(1))
                .withTransactionType("PAYMENT")
                .insert(rule.getJdbi());
        TransactionFixture refund = aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId)
                .withTransactionType("REFUND")
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId + "different-id")
                .withTransactionType("PAYMENT")
                .insert(rule.getJdbi());

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setGatewayPayoutId(gatewayPayoutId);

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(2));
        assertThat(transactionList.get(0).getExternalId(), is(refund.getExternalId()));
        assertThat(transactionList.get(0).getGatewayPayoutId(), is(refund.getGatewayPayoutId()));
        assertThat(transactionList.get(1).getExternalId(), is(transaction.getExternalId()));
        assertThat(transactionList.get(1).getGatewayPayoutId(), is(transaction.getGatewayPayoutId()));
    }

    @Test
    public void shouldNotFilterByGatewayAccountIdWhenNotSpecified() {
        String gatewayAccountId = "account-id-" + nextLong();

        TransactionFixture mostRecent = aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType("PAYMENT")
                .withCreatedDate(now())
                .insert(rule.getJdbi());

        TransactionFixture earlier = aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId + "different_account")
                .withTransactionType("PAYMENT")
                .withCreatedDate(now().minusHours(1))
                .insert(rule.getJdbi());
        TransactionSearchParams searchParams = new TransactionSearchParams();

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(2));
        assertThat(transactionList.get(0).getExternalId(), is(mostRecent.getExternalId()));
        assertThat(transactionList.get(1).getExternalId(), is(earlier.getExternalId()));
    }

    @Test
    public void searchTransactionsByCursor() {

        transactionFixture = aTransactionFixture()
                .withGatewayAccountId("1")
                .withReference("ref1")
                .withCardholderName("test 1")
                .withLastDigitsCardNumber("1234")
                .withCardBrand("visa")
                .withEmail("test@example.org")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(1))
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withGatewayAccountId("1")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(1))
                .insert(rule.getJdbi());

        searchParams.setAccountIds(List.of("1"));
        searchParams.setReference("ref");
        searchParams.setCardHolderName("test");
        searchParams.setEmail("test@example.org");
        searchParams.setCardBrands(new CommaDelimitedSetParameter("visa"));

        List<TransactionEntity> transactionList = transactionDao.cursorTransactionSearch(searchParams, null, null);

        assertThat(transactionList.size(), is(1));
    }

    @Test
    public void searchTransactionsByMultipleGatewayAccounts() {
        aTransactionFixture()
                .withGatewayAccountId("1")
                .withExternalId("ex-1")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(1))
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withGatewayAccountId("1")
                .withExternalId("ex-2")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(1))
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withGatewayAccountId("2")
                .withExternalId("ex-3")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(1))
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withGatewayAccountId("1")
                .withExternalId("ex-4")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(1))
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withGatewayAccountId("1337")
                .withExternalId("ex-5")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(1))
                .insert(rule.getJdbi());

        searchParams.setAccountIds(List.of("1", "2"));

        var searchParamsList = List.of("ex-1", "ex-2", "ex-3", "ex-4");
        var doNotIncludeList = List.of("ex-5");

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), is(4));
        assertThat(transactionList.stream().filter(x -> searchParamsList.contains(x.getExternalId())).count(), is(4L));
        assertThat(transactionList.stream().filter(x -> doNotIncludeList.contains(x.getExternalId())).count(), is(0L));
    }

    @Test
    public void searchTransactionsByCursor_shouldSplitCursorPages() {
        transactionFixture = aTransactionFixture()
                .withId(9L)
                .withGatewayAccountId("1")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(3))
                .insert(rule.getJdbi());

        TransactionFixture transactionFixture2 = aTransactionFixture()
                .withId(6L)
                .withGatewayAccountId("1")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(5))
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withId(50L)
                .withGatewayAccountId("1")
                .withCreatedDate(transactionFixture2.getCreatedDate())
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withId(200L)
                .withGatewayAccountId("1")
                .withCreatedDate(transactionFixture2.getCreatedDate())
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withId(3L)
                .withGatewayAccountId("1")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(6))
                .insert(rule.getJdbi());

        searchParams.setAccountIds(List.of("1"));
        searchParams.setDisplaySize(2L);

        List<TransactionEntity> firstPage = transactionDao.cursorTransactionSearch(searchParams, null, null);
        TransactionEntity firstLastEntity = firstPage.get(firstPage.size() - 1);

        List<TransactionEntity> secondPage = transactionDao.cursorTransactionSearch(searchParams, firstLastEntity.getCreatedDate(), firstLastEntity.getId());
        TransactionEntity secondLastEntity = secondPage.get(secondPage.size() - 1);

        List<TransactionEntity> thirdPage = transactionDao.cursorTransactionSearch(searchParams, secondLastEntity.getCreatedDate(), secondLastEntity.getId());
        TransactionEntity thirdLastEntity = thirdPage.get(thirdPage.size() - 1);

        List<TransactionEntity> fourthPage = transactionDao.cursorTransactionSearch(searchParams, thirdLastEntity.getCreatedDate(), thirdLastEntity.getId());

        assertThat(firstPage.size(), is(2));
        assertThat(firstLastEntity.getId(), is(200L));
        assertThat(secondPage.size(), is(2));
        assertThat(thirdPage.size(), is(1));
        assertThat(thirdLastEntity.getId(), is(3L));
        assertThat(fourthPage.size(), is(0));
    }

    @Test
    public void searchTransactionsByCursorAndetadataValue() {
        String gatewayAccountId = "account-id-" + nextLong();
        String reference = randomAlphanumeric(15);

        TransactionEntity transaction1 = insertTransaction(gatewayAccountId, reference, ZonedDateTime.now().minusDays(2),
                ImmutableMap.of("test-key-1", "value1", "test-key-2", "value2"));
        TransactionEntity transaction2 = insertTransaction(gatewayAccountId, reference, ZonedDateTime.now(),
                ImmutableMap.of("test-key-n", "VALUE1"));
        TransactionEntity transactionToBeExcluded = insertTransaction(gatewayAccountId, "ref123", ZonedDateTime.now(),
                ImmutableMap.of("test-key-3", "value3"));
        List<Transaction> transactionsToExclude = aPersistedTransactionList(gatewayAccountId, 15, rule.getJdbi(), true);

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setReference(reference);
        searchParams.setMetadataValue("value1");

        List<TransactionEntity> transactionList = transactionDao.cursorTransactionSearch(searchParams, null, null);

        assertThat(transactionList.size(), Matchers.is(2));

        assertThat(transactionList.get(0).getExternalId(), is(transaction2.getExternalId()));
        JsonElement externalMetadata = JsonParser.parseString(transactionList.get(0).getTransactionDetails()).getAsJsonObject().get("external_metadata");
        assertThat(externalMetadata.getAsJsonObject().get("test-key-n").getAsString(), is("VALUE1"));

        assertThat(transactionList.get(1).getExternalId(), is(transaction1.getExternalId()));
        externalMetadata = JsonParser.parseString(transactionList.get(1).getTransactionDetails()).getAsJsonObject().get("external_metadata");
        assertThat(externalMetadata.getAsJsonObject().get("test-key-1").getAsString(), is("value1"));
        assertThat(externalMetadata.getAsJsonObject().get("test-key-2").getAsString(), is("value2"));
    }

    @Test
    public void getTotalWithLimitForSearchShouldApplyLimitTotalSizeCorrectly() {
        String gatewayAccountId = "account-id-" + nextLong();

        aPersistedTransactionList(gatewayAccountId, 15, rule.getJdbi(), true);
        aTransactionFixture().withGatewayAccountId(gatewayAccountId).withReference("to_exclude")
                .insert(rule.getJdbi());
        aTransactionFixture().withGatewayAccountId(randomAlphanumeric(26)).withReference("to_exclude")
                .insert(rule.getJdbi());

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountIds(List.of(gatewayAccountId));
        searchParams.setReference("reference");
        searchParams.setEmail("example.org");
        searchParams.setCardHolderName("smith");
        searchParams.setLastDigitsCardNumber("1234");
        searchParams.setFromDate("2019-10-01T10:00:00.000Z");
        searchParams.setToDate(now().plusDays(2).toString());
        searchParams.setLimitTotalSize(15L);

        Long total = transactionDao.getTotalWithLimitForSearch(searchParams);
        assertThat(total, is(15L));
    }

    @Test
    public void getTotalWithLimitForSearchShouldApplySearchByMetadataValueCorrectly() {
        String gatewayAccountId = "account-id-" + nextLong();
        String reference = randomAlphanumeric(15);

        insertTransaction(gatewayAccountId, reference, ZonedDateTime.now().minusDays(2),
                ImmutableMap.of("test-key-1", "value1", "test-key-2", "value2"));
        insertTransaction(gatewayAccountId, reference, ZonedDateTime.now(),
                ImmutableMap.of("test-key-n", "VALUE1"));
        List<Transaction> transactionsToExclude = aPersistedTransactionList(gatewayAccountId, 15, rule.getJdbi(), true);

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setReference(reference);
        searchParams.setMetadataValue("value1");

        Long total = transactionDao.getTotalWithLimitForSearch(searchParams);
        assertThat(total, is(2L));
    }

    @Test
    public void shouldGetAndMapTransactionWithPaidOutDateCorrectly() {
        String gatewayPayoutId = randomAlphanumeric(15);

        transactionFixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withMoto(true)
                .withGatewayPayoutId(gatewayPayoutId)
                .insert(rule.getJdbi());

        var payoutFixture = aPayoutFixture()
                .withGatewayAccountId(transactionFixture.getGatewayAccountId())
                .withGatewayPayoutId(gatewayPayoutId)
                .build()
                .insert(rule.getJdbi())
                .toEntity();

        searchParams.setAccountIds(List.of(transactionFixture.getGatewayAccountId()));

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(1));
        TransactionEntity transaction = transactionList.get(0);

        assertThat(transaction.getId(), is(transactionFixture.getId()));
        assertThat(transaction.getGatewayAccountId(), is(transactionFixture.getGatewayAccountId()));
        assertThat(transaction.getExternalId(), is(transactionFixture.getExternalId()));
        assertThat(transaction.getAmount(), is(transactionFixture.getAmount()));
        assertThat(transaction.getReference(), is(transactionFixture.getReference()));
        assertThat(transaction.getDescription(), is(transactionFixture.getDescription()));
        assertThat(transaction.getState(), is(transactionFixture.getState()));
        assertThat(transaction.getEmail(), is(transactionFixture.getEmail()));
        assertThat(transaction.getCardholderName(), is(transactionFixture.getCardDetails().getCardHolderName()));
        assertThat(transaction.getCardBrand(), is(transactionFixture.getCardDetails().getCardBrand()));
        assertThat(transaction.getCreatedDate(), is(transactionFixture.getCreatedDate()));
        assertThat(transaction.isMoto(), is(transactionFixture.isMoto()));
        assertThat(transaction.getPayoutEntity().get().getPaidOutDate(), is(payoutFixture.getPaidOutDate()));

        Long total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(1L));
    }

    @Test
    public void shouldGetAndMapTransactionWithPaidOutDateCorrectlyWhenNoGatewayAccount() {
        String gatewayPayoutId = randomAlphanumeric(15);

        transactionFixture = aTransactionFixture()
                .withDefaultCardDetails()
                .withMoto(true)
                .withGatewayPayoutId(gatewayPayoutId)
                .insert(rule.getJdbi());

        var payoutFixture = aPayoutFixture()
                .withGatewayAccountId(transactionFixture.getGatewayAccountId())
                .withGatewayPayoutId(gatewayPayoutId)
                .build()
                .insert(rule.getJdbi())
                .toEntity();

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(1));
        TransactionEntity transaction = transactionList.get(0);

        assertThat(transaction.getPayoutEntity().get().getPaidOutDate(), is(payoutFixture.getPaidOutDate()));

        Long total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(1L));
    }

    @Test
    public void searchTransactionsByCursorWithPaidoutDate() {
        String gatewayPayoutId = randomAlphanumeric(15);

        transactionFixture = aTransactionFixture()
                .withGatewayAccountId("1")
                .withReference("ref1")
                .withCardholderName("test 1")
                .withLastDigitsCardNumber("1234")
                .withCardBrand("visa")
                .withEmail("test@example.org")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(1))
                .withGatewayPayoutId(gatewayPayoutId)
                .insert(rule.getJdbi());

        var payoutFixture = aPayoutFixture()
                .withGatewayAccountId(transactionFixture.getGatewayAccountId())
                .withGatewayPayoutId(gatewayPayoutId)
                .build()
                .insert(rule.getJdbi())
                .toEntity();

        List<TransactionEntity> transactionList = transactionDao.cursorTransactionSearch(searchParams, null, null);

        assertThat(transactionList.size(), is(1));
        assertThat(transactionList.get(0).getPayoutEntity().get().getPaidOutDate(), is(payoutFixture.getPaidOutDate()));
    }

    @Test
    public void shouldReturn1Records_WhenSearchingBySettledDate() {
        String gatewayAccountId = "account-id-" + nextLong();
        String gatewayPayoutId1 = randomAlphanumeric(20);
        String gatewayPayoutId2 = randomAlphanumeric(20);
        String paidoutDate = "2020-09-08T00:04:08.000Z";

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId1)
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withGatewayPayoutId(gatewayPayoutId2)
                .insert(rule.getJdbi());

        aPayoutFixture()
                .withGatewayPayoutId(gatewayPayoutId1)
                .withPaidOutDate(ZonedDateTime.parse(paidoutDate).minusMinutes(5L))
                .withGatewayAccountId(gatewayAccountId)
                .build()
                .insert(rule.getJdbi());

        aPayoutFixture()
                .withGatewayPayoutId(gatewayPayoutId2)
                .withPaidOutDate(ZonedDateTime.parse(paidoutDate).plusDays(1L))
                .withGatewayAccountId(gatewayAccountId)
                .build()
                .insert(rule.getJdbi());


        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setFromSettledDate("2020-09-08");

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);
        assertThat(transactionList.size(), is(1));

        Long total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(1L));

        searchParams = new TransactionSearchParams();
        searchParams.setToSettledDate("2020-09-08");

        transactionList = transactionDao.searchTransactions(searchParams);
        assertThat(transactionList.size(), is(1));

        total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(1L));

        searchParams = new TransactionSearchParams();
        searchParams.setFromSettledDate("2020-09-08");
        searchParams.setToSettledDate("2020-09-10");

        transactionList = transactionDao.searchTransactions(searchParams);
        assertThat(transactionList.size(), is(1));

        total = transactionDao.getTotalForSearch(searchParams);
        assertThat(total, is(1L));
    }

    private TransactionEntity insertTransaction(String gatewayAccountId, String reference, ZonedDateTime createdDate,
                                                Map<String, Object> externalMetadata) {
        return aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withTransactionType("PAYMENT")
                .withReference(reference)
                .withCreatedDate(createdDate)
                .withGatewayAccountId(gatewayAccountId)
                .withExternalMetadata(externalMetadata)
                .withDefaultTransactionDetails()
                .insertTransactionAndTransactionMetadata(rule.getJdbi())
                .toEntity();
    }
}