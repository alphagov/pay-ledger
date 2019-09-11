package uk.gov.pay.ledger.pact;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TransactionsAndEventApiContractTest.class,
        PaymentCreatedEventQueueContractTest.class,
        PaymentDetailsEnteredEventQueueContractTest.class,
        CaptureConfirmedEventQueueContractTest.class,
        RefundCreatedByUserEventQueueContractTest.class,
        RefundSubmittedEventQueueContractTest.class,
        RefundSucceededEventQueueContractTest.class,
        CaptureSubmittedEventQueueContractTest.class
})
public class ContractTestSuite {

}
