package uk.gov.pay.ledger.transaction.dao;


import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.search.common.CommaDelimitedSetParameter;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.DatabaseTestHelper;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static java.time.ZonedDateTime.now;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class TransactionDaoSearchIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private TransactionFixture transactionFixture;
    private TransactionDao transactionDao;
    private TransactionSearchParams searchParams;

    private DatabaseTestHelper databaseTestHelper = aDatabaseTestHelper(rule.getJdbi());

    @Before
    public void setUp() {
        databaseTestHelper.truncateAllData();
        transactionDao = new TransactionDao(rule.getJdbi());
        searchParams = new TransactionSearchParams();
    }

    @Test
    public void shouldGetAndMapTransactionCorrectly() {

        transactionFixture = aTransactionFixture()
                .withDefaultCardDetails()
                .insert(rule.getJdbi());

        searchParams.setAccountId(transactionFixture.getGatewayAccountId());

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
        searchParams.setAccountId(gatewayAccountId);

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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);
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
        searchParams.setAccountId(gatewayAccountId);

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
        searchParams.setAccountId(gatewayAccountId);

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(1));
        assertThat(transactionList.get(0).getExternalId(), is(transaction.getExternalId()));
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
    public void searchTransactionsAndParent_shouldSearchByGatewayAccountIdOnTransactionAndParentTransaction() {
        transactionFixture = insertTransaction();
        TransactionFixture transactionFixtureChild = insertRefundChildTransaction(transactionFixture,
                now(ZoneOffset.UTC).plusDays(1));
        TransactionFixture transactionThatShouldBeExcluded = insertTransaction();

        searchParams.setAccountId(transactionFixture.getGatewayAccountId());
        List<TransactionEntity> transactionList = transactionDao.searchTransactionsAndParent(searchParams);
        assertThat(transactionList.size(), is(2));

        assertTransactionEquals(transactionList.get(0), transactionFixtureChild);
        assertTransactionEquals(transactionList.get(0).getParentTransactionEntity(), transactionFixture);
        assertTransactionEquals(transactionList.get(1), transactionFixture);

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);
        assertThat(total, is(2L));
    }

    @Test
    public void searchTransactionsAndParent_shouldSearchByEmailOnTransactionAndParentTransaction() {
        transactionFixture = aTransactionFixture()
                .withEmail("test-email")
                .insert(rule.getJdbi());
        TransactionFixture transactionFixtureChild = insertRefundChildTransaction(transactionFixture,
                now(ZoneOffset.UTC).plusDays(1));

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(transactionFixture.getGatewayAccountId());
        searchParams.setEmail("test");

        List<TransactionEntity> transactionList = transactionDao.searchTransactionsAndParent(searchParams);
        assertThat(transactionList.size(), is(2));

        assertTransactionEquals(transactionList.get(0), transactionFixtureChild);
        assertTransactionEquals(transactionList.get(0).getParentTransactionEntity(), transactionFixture);
        assertTransactionEquals(transactionList.get(1), transactionFixture);

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);
        assertThat(total, is(2L));
    }

    @Test
    public void searchTransactionsAndParent_shouldSearchByPartialReferenceOnTransactionAndParentTransaction() {
        transactionFixture = aTransactionFixture()
                .withReference("reference")
                .insert(rule.getJdbi());
        TransactionFixture transactionFixtureChild = insertRefundChildTransaction(transactionFixture,
                now(ZoneOffset.UTC).plusDays(1));

        searchParams.setAccountId(transactionFixture.getGatewayAccountId());
        searchParams.setReference("ref");
        List<TransactionEntity> transactionList = transactionDao.searchTransactionsAndParent(searchParams);
        assertThat(transactionList.size(), is(2));

        assertTransactionEquals(transactionList.get(0), transactionFixtureChild);
        assertTransactionEquals(transactionList.get(0).getParentTransactionEntity(), transactionFixture);
        assertTransactionEquals(transactionList.get(1), transactionFixture);

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);
        assertThat(total, is(2L));
    }

    @Test
    public void searchTransactionsAndParent_shouldSearchByExactReferenceOnTransactionAndParentTransaction() {
        transactionFixture = aTransactionFixture()
                .withReference("reference")
                .insert(rule.getJdbi());
        TransactionFixture transactionFixtureChild = insertRefundChildTransaction(transactionFixture,
                now(ZoneOffset.UTC).plusDays(1));
        TransactionFixture transactionToExclude = aTransactionFixture()
                .withReference("reference2")
                .withGatewayAccountId(transactionFixture.getGatewayAccountId())
                .insert(rule.getJdbi());

        searchParams.setAccountId(transactionFixture.getGatewayAccountId());
        searchParams.setExactReferenceMatch(true);
        searchParams.setReference("reference");
        List<TransactionEntity> transactionList = transactionDao.searchTransactionsAndParent(searchParams);
        assertThat(transactionList.size(), is(2));

        assertTransactionEquals(transactionList.get(0), transactionFixtureChild);
        assertTransactionEquals(transactionList.get(0).getParentTransactionEntity(), transactionFixture);
        assertTransactionEquals(transactionList.get(1), transactionFixture);

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);
        assertThat(total, is(2L));
    }

    @Test
    public void searchTransactionsAndParent_shouldSearchByCardholderNameOnTransactionAndParentTransaction() {
        transactionFixture = aTransactionFixture()
                .withCardholderName("Mr Test")
                .insert(rule.getJdbi());
        TransactionFixture transactionFixtureChild = insertRefundChildTransaction(transactionFixture,
                now(ZoneOffset.UTC).plusDays(1));

        searchParams.setAccountId(transactionFixture.getGatewayAccountId());
        searchParams.setCardHolderName("mr");
        List<TransactionEntity> transactionList = transactionDao.searchTransactionsAndParent(searchParams);
        assertThat(transactionList.size(), is(2));

        assertTransactionEquals(transactionList.get(0), transactionFixtureChild);
        assertTransactionEquals(transactionList.get(0).getParentTransactionEntity(), transactionFixture);
        assertTransactionEquals(transactionList.get(1), transactionFixture);

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);
        assertThat(total, is(2L));
    }

    @Test
    public void searchTransactionsAndParent_shouldSearchByLastDigitsCardNumberNameOnTransactionAndParentTransaction() {
        transactionFixture = aTransactionFixture()
                .withLastDigitsCardNumber("4242")
                .insert(rule.getJdbi());
        TransactionFixture transactionFixtureChild = insertRefundChildTransaction(transactionFixture,
                now(ZoneOffset.UTC).plusDays(1));

        searchParams.setAccountId(transactionFixture.getGatewayAccountId());
        searchParams.setLastDigitsCardNumber("4242");
        List<TransactionEntity> transactionList = transactionDao.searchTransactionsAndParent(searchParams);
        assertThat(transactionList.size(), is(2));

        assertTransactionEquals(transactionList.get(0), transactionFixtureChild);
        assertTransactionEquals(transactionList.get(0).getParentTransactionEntity(), transactionFixture);
        assertTransactionEquals(transactionList.get(1), transactionFixture);

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);
        assertThat(total, is(2L));
    }

    @Test
    public void searchTransactionsAndParent_shouldSearchByCardBrandOnTransactionAndParentTransaction() {
        transactionFixture = aTransactionFixture()
                .withCardBrand("visa")
                .insert(rule.getJdbi());
        TransactionFixture transactionFixtureChild = insertRefundChildTransaction(transactionFixture,
                now(ZoneOffset.UTC).plusDays(2));

        searchParams.setAccountId(transactionFixture.getGatewayAccountId());
        searchParams.setCardBrands(new CommaDelimitedSetParameter("visa"));

        List<TransactionEntity> transactionList = transactionDao.searchTransactionsAndParent(searchParams);
        assertThat(transactionList.size(), is(2));
        assertTransactionEquals(transactionList.get(0), transactionFixtureChild);
        assertTransactionEquals(transactionList.get(0).getParentTransactionEntity(), transactionFixture);
        assertTransactionEquals(transactionList.get(1), transactionFixture);

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);
        assertThat(total, is(2L));
    }

    @Test
    public void searchTransactionsAndParent_shouldSearchByFromDate() {
        transactionFixture = aTransactionFixture()
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(1))
                .insert(rule.getJdbi());
        TransactionFixture transactionFixtureChild = insertRefundChildTransaction(transactionFixture,
                now(ZoneOffset.UTC).plusDays(1));

        searchParams.setAccountId(transactionFixture.getGatewayAccountId());
        searchParams.setFromDate(now(ZoneOffset.UTC).toString());
        List<TransactionEntity> transactionList = transactionDao.searchTransactionsAndParent(searchParams);
        assertThat(transactionList.size(), is(1));

        assertTransactionEquals(transactionList.get(0), transactionFixtureChild);
        assertTransactionEquals(transactionList.get(0).getParentTransactionEntity(), transactionFixture);

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);
        assertThat(total, is(1L));
    }

    @Test
    public void searchTransactionsAndParent_shouldSearchByToDate() {
        transactionFixture = aTransactionFixture()
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(1))
                .insert(rule.getJdbi());
        insertRefundChildTransaction(transactionFixture, now(ZoneOffset.UTC).plusDays(1));

        searchParams.setAccountId(transactionFixture.getGatewayAccountId());
        searchParams.setToDate(now(ZoneOffset.UTC).toString());
        List<TransactionEntity> transactionList = transactionDao.searchTransactionsAndParent(searchParams);
        assertThat(transactionList.size(), is(1));

        assertTransactionEquals(transactionList.get(0), transactionFixture);

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);
        assertThat(total, is(1L));
    }

    @Test
    public void searchTransactionsAndParent_withPageSize10() {
        String gatewayAccountId = "account-id-" + nextLong();

        for (int i = 1; i <= 20; i++) {
            aTransactionFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .insert(rule.getJdbi());
        }

        searchParams.setAccountId(gatewayAccountId);
        searchParams.setDisplaySize(10L);

        List<TransactionEntity> transactionList = transactionDao.searchTransactionsAndParent(searchParams);
        assertThat(transactionList.size(), Matchers.is(10));

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);
        assertThat(total, is(20L));
    }

    @Test
    public void searchTransactionsAndParent_withOffsetAndPageSize() {
        transactionFixture = insertTransaction();
        TransactionFixture transactionFixtureChild = insertRefundChildTransaction(transactionFixture,
                now(ZoneOffset.UTC).plusDays(1));
        aTransactionFixture()
                .withGatewayAccountId(transactionFixture.getGatewayAccountId())
                .withCreatedDate(now(ZoneOffset.UTC).plusDays(2))
                .insert(rule.getJdbi());

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(transactionFixture.getGatewayAccountId());
        searchParams.setDisplaySize(1L);
        searchParams.setPageNumber(2L);

        List<TransactionEntity> transactionList = transactionDao.searchTransactionsAndParent(searchParams);
        assertThat(transactionList.size(), is(1));
        assertTransactionEquals(transactionList.get(0), transactionFixtureChild);

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);
        assertThat(total, is(3L));
    }

    @Test
    public void searchTransactionsAndParent_shouldSearchByPaymentStates() {
        transactionFixture = insertTransaction();
        aTransactionFixture().withState(TransactionState.FAILED_REJECTED)
                .withGatewayAccountId(transactionFixture.getGatewayAccountId())
                .withTransactionType("PAYMENT")
                .insert(rule.getJdbi());
        insertRefundChildTransaction(transactionFixture, now(ZoneOffset.UTC).plusDays(2));

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(transactionFixture.getGatewayAccountId());
        searchParams.setPaymentStates(new CommaDelimitedSetParameter("created"));

        List<TransactionEntity> transactionList = transactionDao.searchTransactionsAndParent(searchParams);
        assertThat(transactionList.size(), is(1));
        assertTransactionEquals(transactionList.get(0), transactionFixture);

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);
        assertThat(total, is(1L));
    }

    @Test
    public void searchTransactionsAndParent_shouldSearchByRefundStates() {
        transactionFixture = insertTransaction();
        aTransactionFixture().withState(TransactionState.FAILED_REJECTED)
                .withGatewayAccountId(transactionFixture.getGatewayAccountId())
                .insert(rule.getJdbi());
        TransactionFixture transactionFixtureChild = insertRefundChildTransaction(transactionFixture,
                now(ZoneOffset.UTC).plusDays(2));

        searchParams.setAccountId(transactionFixture.getGatewayAccountId());
        searchParams.setRefundStates(new CommaDelimitedSetParameter("created"));

        List<TransactionEntity> transactionList = transactionDao.searchTransactionsAndParent(searchParams);
        assertThat(transactionList.size(), is(1));
        assertTransactionEquals(transactionList.get(0), transactionFixtureChild);

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);
        assertThat(total, is(1L));
    }

    @Test
    public void searchTransactionsAndParent_shouldSearchByFirstDigitsCardNumber() {
        transactionFixture = aTransactionFixture()
                .withFirstDigitsCardNumber("424242")
                .insert(rule.getJdbi());
        insertRefundChildTransaction(transactionFixture, now(ZoneOffset.UTC).plusDays(2));

        searchParams.setAccountId(transactionFixture.getGatewayAccountId());
        searchParams.setFirstDigitsCardNumber("424242");

        List<TransactionEntity> transactionList = transactionDao.searchTransactionsAndParent(searchParams);
        assertThat(transactionList.size(), is(1));
        assertTransactionEquals(transactionList.get(0), transactionFixture);

        Long total = transactionDao.getTotalForSearchTransactionAndParent(searchParams);
        assertThat(total, is(1L));
    }

    @Test
    public void searchTransactionsByCursor() {

        transactionFixture = aTransactionFixture()
                .withGatewayAccountId("1")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(1))
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withGatewayAccountId("1")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(1))
                .insert(rule.getJdbi());

        searchParams.setAccountId("1");

        List<TransactionEntity> transactionList = transactionDao.cursorTransactionSearch(searchParams, null, null);

        assertThat(transactionList.size(), is(2));
    }

    @Test
    public void searchTransactionsByCursor_shouldSplitCursorPages() {
        transactionFixture = aTransactionFixture()
                .withId(9L)
                .withGatewayAccountId("1")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(3))
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withId(6L)
                .withGatewayAccountId("1")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(4))
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withId(50L)
                .withGatewayAccountId("1")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(5))
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withId(200L)
                .withGatewayAccountId("1")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(5))
                .insert(rule.getJdbi());

        aTransactionFixture()
                .withId(3L)
                .withGatewayAccountId("1")
                .withCreatedDate(now(ZoneOffset.UTC).minusDays(6))
                .insert(rule.getJdbi());

        searchParams.setAccountId("1");
        searchParams.setDisplaySize(2L);

        List<TransactionEntity> firstPage = transactionDao.cursorTransactionSearch(searchParams, null, null);
        TransactionEntity firstLastEntity = firstPage.get(firstPage.size() - 1);

        List<TransactionEntity> secondPage = transactionDao.cursorTransactionSearch(searchParams, firstLastEntity.getCreatedDate(), firstLastEntity.getId());
        TransactionEntity secondLastEntity = secondPage.get(secondPage.size() - 1);

        List<TransactionEntity> thirdPage = transactionDao.cursorTransactionSearch(searchParams, secondLastEntity.getCreatedDate(), secondLastEntity.getId());
        TransactionEntity thirdLastEntity = thirdPage.get(thirdPage.size() - 1);

        List<TransactionEntity> fourthPage = transactionDao.cursorTransactionSearch(searchParams, thirdLastEntity.getCreatedDate(), thirdLastEntity.getId());

        assertThat(firstPage.size(), is(2));
        assertThat(firstLastEntity.getId(), is(6L));
        assertThat(secondPage.size(), is (2));
        assertThat(thirdPage.size(), is(1));
        assertThat(thirdLastEntity.getId(), is(3L));
        assertThat(fourthPage.size(), is(0));
    }

    private void assertTransactionEquals(TransactionEntity actualTransactionEntity, TransactionFixture transactionFixture) {
        assertThat(actualTransactionEntity.getId(), is(transactionFixture.getId()));
        assertThat(actualTransactionEntity.getGatewayAccountId(), is(transactionFixture.getGatewayAccountId()));
        assertThat(actualTransactionEntity.getExternalId(), is(transactionFixture.getExternalId()));
        assertThat(actualTransactionEntity.getParentExternalId(), is(transactionFixture.getParentExternalId()));
        assertThat(actualTransactionEntity.getAmount(), is(transactionFixture.getAmount()));
        assertThat(actualTransactionEntity.getReference(), is(transactionFixture.getReference()));
        assertThat(actualTransactionEntity.getDescription(), is(transactionFixture.getDescription()));
        assertThat(actualTransactionEntity.getState(), is(transactionFixture.getState()));
        assertThat(actualTransactionEntity.getEmail(), is(transactionFixture.getEmail()));
        if (transactionFixture.getCardDetails() != null) {
            assertThat(actualTransactionEntity.getCardholderName(), is(transactionFixture.getCardDetails().getCardHolderName()));
            assertThat(actualTransactionEntity.getCardBrand(), is(transactionFixture.getCardDetails().getCardBrand()));
        }
        assertThat(actualTransactionEntity.getCreatedDate(), is(transactionFixture.getCreatedDate()));
        assertThat(actualTransactionEntity.getCardBrand(), is(transactionFixture.getCardBrand()));
        assertThat(actualTransactionEntity.getLastDigitsCardNumber(), is(transactionFixture.getLastDigitsCardNumber()));
        assertThat(actualTransactionEntity.getFirstDigitsCardNumber(), is(transactionFixture.getFirstDigitsCardNumber()));
    }

    private TransactionFixture insertTransaction() {
        return aTransactionFixture()
                .withState(TransactionState.CREATED)
                .withDefaultCardDetails()
                .insert(rule.getJdbi());
    }

    private TransactionFixture insertRefundChildTransaction(TransactionFixture parentTransactionFixture, ZonedDateTime createdDate) {
        return aTransactionFixture()
                .withParentExternalId(parentTransactionFixture.getExternalId())
                .withGatewayAccountId(parentTransactionFixture.getGatewayAccountId())
                .withTransactionType("REFUND")
                .withState(TransactionState.CREATED)
                .withCreatedDate(createdDate)
                .insert(rule.getJdbi());
    }
}