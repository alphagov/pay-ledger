package uk.gov.pay.ledger.app;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.pay.ledger.event.EventDao;
import uk.gov.pay.ledger.event.EventResource;

public class LedgerApp extends Application<LedgerConfiguration> {

    public static void main(String[] args) throws Exception {
        new LedgerApp().run(args);
    }

    @Override
    public void initialize(Bootstrap<LedgerConfiguration> bootstrap){
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false))
        );

        bootstrap.addBundle(new MigrationsBundle<LedgerConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(LedgerConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });
    }

    @Override
    public void run(LedgerConfiguration ledgerConfiguration, Environment environment) {
        EventDao dao = new EventDao();
        environment.jersey().register(new EventResource(dao));
    }
}
