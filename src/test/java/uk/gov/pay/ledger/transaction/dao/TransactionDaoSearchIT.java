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

import java.time.ZonedDateTime;
import java.util.List;

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
    public void shouldReturn1Record_whenSearchingByReference() {

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
        searchParams.setReference("reference 1");

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
                    .withCreatedDate(ZonedDateTime.now().plusSeconds(1L))
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
                    .withCreatedDate(ZonedDateTime.now().minusDays(i))
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setFromDate(ZonedDateTime.now().minusDays(1).minusMinutes(10).toString());

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
                    .withCreatedDate(ZonedDateTime.now().minusDays(i))
                    .insert(rule.getJdbi());
        }

        TransactionSearchParams searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);
        searchParams.setToDate(ZonedDateTime.now().minusDays(2).plusMinutes(10).toString());

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
                .withCreatedDate(ZonedDateTime.now())
                .insert(rule.getJdbi());

        TransactionFixture earlier =  aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId + "different_account")
                .withTransactionType("PAYMENT")
                .withCreatedDate(ZonedDateTime.now().minusHours(1))
                .insert(rule.getJdbi());
        TransactionSearchParams searchParams = new TransactionSearchParams();

        List<TransactionEntity> transactionList = transactionDao.searchTransactions(searchParams);

        assertThat(transactionList.size(), Matchers.is(2));
        assertThat(transactionList.get(0).getExternalId(), is(mostRecent.getExternalId()));
        assertThat(transactionList.get(1).getExternalId(), is(earlier.getExternalId()));
    }
}