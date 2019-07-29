package uk.gov.pay.ledger.pact;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TransactionsApiContractTest.class,
        PaymentCreatedEventQueueContractTest.class,
        CaptureConfirmedEventQueueContractTest.class,
        GetTransactionContractTest.class,
        RefundCreatedByUserEventQueueContractTest.class,
        RefundSubmittedEventQueueContractTest.class,
        RefundSucceededEventQueueContractTest.class
})
public class ContractTestSuite {

}
