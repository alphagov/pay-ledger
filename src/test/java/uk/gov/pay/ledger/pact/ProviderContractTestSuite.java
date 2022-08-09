package uk.gov.pay.ledger.pact;

import com.google.common.collect.ImmutableSetMultimap;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import uk.gov.service.payments.commons.testing.pact.provider.CreateTestSuite;

@RunWith(AllTests.class)
public class ProviderContractTestSuite {

    public static TestSuite suite() {
        ImmutableSetMultimap.Builder<String, JUnit4TestAdapter> consumerToJUnitTest = ImmutableSetMultimap.builder();
        consumerToJUnitTest.put("publicapi", new JUnit4TestAdapter(PublicApiContractTest.class));
        consumerToJUnitTest.put("selfservice", new JUnit4TestAdapter(SelfServiceContractTest.class));
        consumerToJUnitTest.put("connector", new JUnit4TestAdapter(ConnectorContractTest.class));
        consumerToJUnitTest.put("adminusers", new JUnit4TestAdapter(AdminusersContractTest.class));
        consumerToJUnitTest.put("webhooks", new JUnit4TestAdapter(WebhooksContractTest.class));
        return CreateTestSuite.create(consumerToJUnitTest.build());
    }
}
