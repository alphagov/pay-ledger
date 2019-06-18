package uk.gov.pay.ledger.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import uk.gov.pay.commons.utils.logging.LoggingFilter;
import uk.gov.pay.ledger.event.resource.EventResource;
import uk.gov.pay.ledger.exception.BadRequestExceptionMapper;
import uk.gov.pay.ledger.healthcheck.HealthCheckResource;
import uk.gov.pay.ledger.queue.managed.QueueMessageReceiver;
import uk.gov.pay.ledger.transaction.resource.TransactionResource;

import static java.util.EnumSet.of;
import static javax.servlet.DispatcherType.REQUEST;

public class LedgerApp extends Application<LedgerConfig> {

    public static void main(String[] args) throws Exception {
        new LedgerApp().run(args);
    }

    @Override
    public void initialize(Bootstrap<LedgerConfig> bootstrap){
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false))
        );

        bootstrap.addBundle(new MigrationsBundle<LedgerConfig>() {
            @Override
            public DataSourceFactory getDataSourceFactory(LedgerConfig configuration) {
                return configuration.getDataSourceFactory();
            }
        });
    }

    @Override
    public void run(LedgerConfig config, Environment environment) {
        final Injector injector = Guice.createInjector(new LedgerModule(config, environment, createJdbi(config.getDataSourceFactory())));

        environment.jersey().register(injector.getInstance(EventResource.class));
        environment.jersey().register(injector.getInstance(TransactionResource.class));
        environment.jersey().register(injector.getInstance(HealthCheckResource.class));
        environment.servlets().addFilter("LoggingFilter", new LoggingFilter())
                .addMappingForUrlPatterns(of(REQUEST), true, "/v1/*");
        environment.jersey().register(new BadRequestExceptionMapper());

        if(config.getQueueMessageReceiverConfig().isBackgroundProcessingEnabled()) {
            environment.lifecycle().manage(injector.getInstance(QueueMessageReceiver.class));
        }
    }

    private Jdbi createJdbi(DataSourceFactory dataSourceFactory) {
        final Jdbi jdbi = Jdbi.create(
                dataSourceFactory.getUrl(),
                dataSourceFactory.getUser(),
                dataSourceFactory.getPassword()
        );
        jdbi.installPlugin(new SqlObjectPlugin());

        return jdbi;
    }
}
