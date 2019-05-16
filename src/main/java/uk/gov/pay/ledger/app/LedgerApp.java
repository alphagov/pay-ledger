package uk.gov.pay.ledger.app;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.pay.ledger.event.EventDao;
import uk.gov.pay.ledger.event.EventResource;

public class LedgerApp extends Application<LedgerConfiguration> {

    public static final boolean NON_STRICT_VARIABLE_SUBSTITUTION = false;

    public static void main(String[] args) throws Exception {
        new LedgerApp().run(args);
    }

    @Override
    public void initialize(Bootstrap<LedgerConfiguration> bootstrap){
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(NON_STRICT_VARIABLE_SUBSTITUTION))
        );

    }

    @Override
    public void run(LedgerConfiguration ledgerConfiguration, Environment environment) throws Exception {
        EventDao dao = new EventDao();
        environment.jersey().register(new EventResource(dao));


    }
}
