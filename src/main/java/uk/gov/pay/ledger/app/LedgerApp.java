package uk.gov.pay.ledger.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.jdbi3.bundles.JdbiExceptionsBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.agreement.resource.AgreementResource;
import uk.gov.pay.ledger.event.resource.EventResource;
import uk.gov.pay.ledger.exception.BadRequestExceptionMapper;
import uk.gov.pay.ledger.exception.JerseyViolationExceptionMapper;
import uk.gov.pay.ledger.filters.LoggingMDCRequestFilter;
import uk.gov.pay.ledger.filters.LoggingMDCResponseFilter;
import uk.gov.pay.ledger.healthcheck.DependentResourceWaitCommand;
import uk.gov.pay.ledger.healthcheck.HealthCheckResource;
import uk.gov.pay.ledger.healthcheck.SQSHealthCheck;
import uk.gov.pay.ledger.payout.resource.PayoutResource;
import uk.gov.pay.ledger.queue.managed.QueueMessageReceiver;
import uk.gov.pay.ledger.report.resource.PerformanceReportResource;
import uk.gov.pay.ledger.report.resource.ReportResource;
import uk.gov.pay.ledger.transaction.resource.TransactionResource;
import uk.gov.service.payments.logging.GovUkPayDropwizardRequestJsonLogLayoutFactory;
import uk.gov.service.payments.logging.LoggingFilter;
import uk.gov.service.payments.logging.LogstashConsoleAppenderFactory;

import java.net.URI;

import static java.util.EnumSet.of;
import static javax.servlet.DispatcherType.REQUEST;

public class LedgerApp extends Application<LedgerConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LedgerApp.class);
    
    public static void main(String[] args) throws Exception {
        new LedgerApp().run(args);
    }

    @Override
    public void initialize(Bootstrap<LedgerConfig> bootstrap){
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false))
        );

        bootstrap.addBundle(new MigrationsBundle<>() {
            @Override
            public DataSourceFactory getDataSourceFactory(LedgerConfig configuration) {
                return configuration.getDataSourceFactory();
            }
        });

        bootstrap.addBundle(new JdbiExceptionsBundle());
        bootstrap.addCommand(new DependentResourceWaitCommand());
        bootstrap.getObjectMapper().getSubtypeResolver().registerSubtypes(LogstashConsoleAppenderFactory.class);
        bootstrap.getObjectMapper().getSubtypeResolver().registerSubtypes(GovUkPayDropwizardRequestJsonLogLayoutFactory.class);
    }

    @Override
    public void run(LedgerConfig config, Environment environment) {
        CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
        collectorRegistry.register(new DropwizardExports(environment.metrics()));
        environment.admin().addServlet("prometheusMetrics", new MetricsServlet(collectorRegistry)).addMapping("/metrics");

        JdbiFactory jdbiFactory = new JdbiFactory();
        final Jdbi jdbi = jdbiFactory.build(environment, config.getDataSourceFactory(), "postgresql");

        final Injector injector = Guice.createInjector(new LedgerModule(config, environment, jdbi));

        environment.jersey().register(injector.getInstance(EventResource.class));
        environment.jersey().register(injector.getInstance(TransactionResource.class));
        environment.jersey().register(injector.getInstance(ReportResource.class));
        environment.jersey().register(injector.getInstance(PerformanceReportResource.class));
        environment.jersey().register(injector.getInstance(AgreementResource.class));
        environment.jersey().register(injector.getInstance(HealthCheckResource.class));

        environment.servlets().addFilter("LoggingFilter", new LoggingFilter())
                .addMappingForUrlPatterns(of(REQUEST), true, "/v1/*");
        environment.jersey().register(injector.getInstance(LoggingMDCRequestFilter.class));
        environment.jersey().register(injector.getInstance(LoggingMDCResponseFilter.class));

        environment.jersey().register(new BadRequestExceptionMapper());
        environment.jersey().register(new JerseyViolationExceptionMapper());
        environment.healthChecks().register("sqsQueue", injector.getInstance(SQSHealthCheck.class));

        if(config.getQueueMessageReceiverConfig().isBackgroundProcessingEnabled()) {
            environment.lifecycle().manage(injector.getInstance(QueueMessageReceiver.class));
        }

        environment.jersey().register(injector.getInstance(PayoutResource.class));
    }
}
