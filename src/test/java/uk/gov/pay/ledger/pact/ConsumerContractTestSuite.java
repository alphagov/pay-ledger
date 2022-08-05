package uk.gov.pay.ledger.pact;

import com.google.common.collect.ImmutableSetMultimap;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import uk.gov.pay.ledger.pact.event.CancelledByUserEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.CaptureConfirmedEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.CaptureSubmittedEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.DisputeCreatedEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.DisputeEvidenceSubmittedEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.DisputeLostEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.DisputeWonEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.FeeIncurredEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.Gateway3dsExemptionResultObtainedEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.Gateway3dsInfoObtainedEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.GatewayRequires3dsAuthorisationEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.PaymentCreatedEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.PaymentDetailsEnteredEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.PaymentIncludedInPayoutEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.PaymentNotificationCreatedEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.PayoutCreatedEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.PayoutFailedEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.PayoutPaidEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.PayoutUpdatedEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.RefundCreatedByUserEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.RefundIncludedInPayoutEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.RefundSubmittedEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.RefundSucceededEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.StatusCorrectedToCapturedToMatchGatewayStatusEventQueueContractTest;
import uk.gov.pay.ledger.pact.event.UserEmailCollectedEventQueueContractTest;
import uk.gov.service.payments.commons.testing.pact.provider.CreateTestSuite;

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
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(StatusCorrectedToCapturedToMatchGatewayStatusEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(FeeIncurredEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(Gateway3dsInfoObtainedEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(GatewayRequires3dsAuthorisationEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(PayoutFailedEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(PayoutPaidEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(PayoutUpdatedEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(DisputeCreatedEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(DisputeWonEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(DisputeEvidenceSubmittedEventQueueContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(DisputeLostEventQueueContractTest.class));

        return CreateTestSuite.create(consumerToJUnitTest.build());
    }

}
