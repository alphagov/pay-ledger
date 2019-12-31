package uk.gov.pay.ledger.performance;

import com.google.gson.Gson;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.performance.dao.PerformanceDao;
import uk.gov.pay.ledger.performance.entity.PerformanceReportEntity;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static java.math.BigDecimal.ZERO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.BigDecimalCloseTo.closeTo;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class PerformanceDaoIT {

    private static Gson gson = new Gson();

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private PerformanceDao transactionDao = new PerformanceDao(rule.getJdbi());

    @Test
    public void report_volume_total_amount_and_average_amount() {
        aTransactionFixture().withAmount(1000L).withState(TransactionState.STARTED).withTransactionType("PAYMENT")
                .withTransactionDetails(toJsonString(Map.of("live", true))).insert(rule.getJdbi());

        aTransactionFixture().withAmount(1000L).withState(TransactionState.SUCCESS).withTransactionType("REFUND")
                .withTransactionDetails(toJsonString(Map.of("live", true))).insert(rule.getJdbi());

        aTransactionFixture().withAmount(1000L).withState(TransactionState.SUCCESS).withTransactionType("PAYMENT")
                .withTransactionDetails(toJsonString(Map.of("live", false))).insert(rule.getJdbi());

        List<Long> relevantAmounts = List.of(1200L, 1020L, 750L);
        relevantAmounts.stream().forEach(amount -> aTransactionFixture()
                .withAmount(amount)
                .withState(TransactionState.SUCCESS)
                .withTransactionType("PAYMENT")
                .withTransactionDetails(toJsonString(Map.of("live", true)))
                .insert(rule.getJdbi()));
        BigDecimal expectedTotalAmount = new BigDecimal(relevantAmounts.stream().mapToLong(amount -> amount).sum());
        BigDecimal expectedAverageAmount = new BigDecimal(relevantAmounts.stream().mapToLong(amount -> amount).average().getAsDouble());

        PerformanceReportEntity performanceReport = transactionDao.performanceReportForPaymentTransactions();
        assertThat(performanceReport.getTotalVolume(), is(3L));
        assertThat(performanceReport.getTotalAmount(), Matchers.is(closeTo(expectedTotalAmount, ZERO)));
        assertThat(performanceReport.getAverageAmount(), Matchers.is(closeTo(expectedAverageAmount, ZERO)));
    }

    private static String toJsonString(Map<String, ?> map) {
        return gson.toJson(map);
    }
}
