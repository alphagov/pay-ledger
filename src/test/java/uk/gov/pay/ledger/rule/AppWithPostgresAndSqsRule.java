package uk.gov.pay.ledger.rule;

import com.amazonaws.services.sqs.AmazonSQS;
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

import java.time.Duration;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

public class AppWithPostgresAndSqsRule extends ExternalResource {
    private static String CONFIG_PATH = resourceFilePath("config/test-config.yaml");
    private final Jdbi jdbi;
    private final AmazonSQS sqsClient;
    private PostgreSQLContainer postgres;
    private DropwizardAppRule<LedgerConfig> appRule;

    public AppWithPostgresAndSqsRule() {
        postgres = (PostgreSQLContainer) new PostgreSQLContainer("postgres:11.1")
                .withStartupTimeout(Duration.ofSeconds(600));
        postgres.start();
        sqsClient = SqsTestDocker.initialise("event-queue");
        appRule = new DropwizardAppRule<>(
                LedgerApp.class,
                CONFIG_PATH,
                config("database.url", postgres.getJdbcUrl()),
                config("database.user", postgres.getUsername()),
                config("database.password", postgres.getPassword()),
                config("sqsConfig.eventQueueUrl", SqsTestDocker.getQueueUrl("event-queue")),
                config("sqsConfig.endpoint", SqsTestDocker.getEndpoint())
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
