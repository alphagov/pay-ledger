package uk.gov.pay.ledger.pact;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import org.junit.runner.RunWith;

@RunWith(PactRunner.class)
@Provider("ledger")
@PactBroker(scheme = "https", host = "${PACT_BROKER_HOST:pact-broker.deploy.payments.service.gov.uk}", tags = {"${PACT_CONSUMER_TAG}", "test-fargate"},
        authentication = @PactBrokerAuth(username = "${PACT_BROKER_USERNAME}", password = "${PACT_BROKER_PASSWORD}"),
        consumers = {"webhooks"})
@IgnoreNoPactsToVerify
@SuppressWarnings("java:S2187")
public class WebhooksContractTest extends ContractTest {
}
