package uk.gov.pay.ledger.rule;

import com.amazonaws.services.sqs.AmazonSQS;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.gov.pay.ledger.app.LedgerApp;
import uk.gov.pay.ledger.app.LedgerConfig;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

public class AppWithPostgresAndSqsRule extends ExternalResource {
    private static String CONFIG_PATH = resourceFilePath("config/test-config.yaml");
    private final Jdbi jdbi;
    private AmazonSQS sqsClient;
    private PostgreSQLContainer postgres;
    private DropwizardAppRule<LedgerConfig> appRule;

    public AppWithPostgresAndSqsRule(ConfigOverride... configOverrides) {
        postgres = new PostgreSQLContainer("postgres:11.1");
        postgres.start();

        sqsClient = SqsTestDocker.initialise("event-queue");

        ConfigOverride[] newConfigOverrides = overrideDatabaseConfig(configOverrides, postgres);
        newConfigOverrides = overrideSqsConfig(newConfigOverrides);

        appRule = new DropwizardAppRule<>(
                LedgerApp.class,
                CONFIG_PATH,
                newConfigOverrides
        );

        jdbi = Jdbi.create(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbi.installPlugin(new SqlObjectPlugin());
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return RuleChain.outerRule(postgres).around(appRule).apply(
                new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        appRule.getApplication().run("db", "migrate", CONFIG_PATH);

                        base.evaluate();
                    }
                }, description);
    }

    private ConfigOverride[] overrideDatabaseConfig(ConfigOverride[] configOverrides, PostgreSQLContainer postgreSQLContainer) {
        List<ConfigOverride> newConfigOverride = newArrayList(configOverrides);
        newConfigOverride.add(config("database.url", postgreSQLContainer.getJdbcUrl()));
        newConfigOverride.add(config("database.user", postgreSQLContainer.getUsername()));
        newConfigOverride.add(config("database.password", postgreSQLContainer.getPassword()));
        return newConfigOverride.toArray(new ConfigOverride[0]);
    }

    private ConfigOverride[] overrideSqsConfig(ConfigOverride[] configOverrides) {
        List<ConfigOverride> newConfigOverride = newArrayList(configOverrides);
        newConfigOverride.add(config("sqsConfig.eventQueueUrl", SqsTestDocker.getQueueUrl("event-queue")));
        newConfigOverride.add(config("sqsConfig.endpoint", SqsTestDocker.getEndpoint()));
        return newConfigOverride.toArray(new ConfigOverride[0]);
    }

    public DropwizardAppRule<LedgerConfig> getAppRule() {
        return appRule;
    }
    public Jdbi getJdbi() {
        return jdbi;
    }

    public AmazonSQS getSqsClient() {
        return sqsClient;
    }
}
