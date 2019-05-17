package uk.gov.pay.ledger.rules;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.gov.pay.ledger.app.LedgerApp;
import uk.gov.pay.ledger.app.LedgerConfig;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

public class AppWithPostgresRule extends ExternalResource {
    private static String CONFIG_PATH = resourceFilePath("config/test-config.yaml");
    private PostgreSQLContainer postgres;
    private DropwizardAppRule<LedgerConfig> appRule;

    public AppWithPostgresRule() {
        postgres = new PostgreSQLContainer();
        postgres.start();
        appRule = new DropwizardAppRule<>(
                LedgerApp.class,
                CONFIG_PATH,
                config("database.url", postgres.getJdbcUrl()),
                config("database.user", postgres.getUsername()),
                config("database.password", postgres.getPassword())
        );
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
}
