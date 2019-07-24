package uk.gov.pay.ledger.pact;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        TransactionsApiContractTest.class,
        EventQueueContractTest.class,
        RefundCreatedByUserContractTest.class,
        GetTransactionContractTest.class
})
public class ContractTestSuite {

}
