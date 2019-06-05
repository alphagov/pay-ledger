package uk.gov.pay.ledger.rules;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.jdbi.v3.core.Jdbi;
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

public class AppWithPostgresRule extends ExternalResource {
    private static String CONFIG_PATH = resourceFilePath("config/test-config.yaml");
    private final Jdbi jdbi;
    private PostgreSQLContainer postgres;
    private DropwizardAppRule<LedgerConfig> appRule;

    public AppWithPostgresRule() {
        postgres = (PostgreSQLContainer) new PostgreSQLContainer("postgres:11.1")
                .withStartupTimeout(Duration.ofSeconds(600));
        postgres.start();
        appRule = new DropwizardAppRule<>(
                LedgerApp.class,
                CONFIG_PATH,
                config("database.url", postgres.getJdbcUrl()),
                config("database.user", postgres.getUsername()),
                config("database.password", postgres.getPassword())
        );

        jdbi = Jdbi.create(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());


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
}
