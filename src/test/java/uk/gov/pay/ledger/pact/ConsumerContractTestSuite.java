package uk.gov.pay.ledger.pact;

import com.google.common.collect.ImmutableSetMultimap;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import uk.gov.pay.commons.testing.pact.provider.CreateTestSuite;

@RunWith(AllTests.class)
public class ConsumerContractTestSuite {

    public static TestSuite suite() {
        ImmutableSetMultimap.Builder<String, JUnit4TestAdapter> consumerToJUnitTest = ImmutableSetMultimap.builder();
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(PaymentCreatedEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(PaymentDetailsEnteredEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(CaptureConfirmedEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(RefundCreatedByUserEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(RefundSubmittedEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(RefundSucceededEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(CaptureSubmittedEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(PaymentNotificationCreatedEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(PayoutCreatedEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(PaymentIncludedInPayoutEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(RefundIncludedInPayoutEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(CancelledByUserEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(UserEmailCollectedEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(Gateway3dsExemptionResultObtainedEventQueueContractTest.class));
        return CreateTestSuite.create(consumerToJUnitTest.build());
    }

}
