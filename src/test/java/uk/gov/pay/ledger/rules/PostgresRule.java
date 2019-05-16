package uk.gov.pay.ledger.rules;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.app.LedgerApp;
import uk.gov.pay.ledger.app.LedgerConfiguration;

import javax.ws.rs.client.Client;
import java.sql.Connection;

public class PostgresRule {

    private Connection connection;

//    @Rule
//    public PostgreSQLContainer postgresContainer = new PostgreSQLContainer();

    private static  String CONFIG_PATH = ResourceHelpers.resourceFilePath("config/test-config.yaml");

//    public static final  DropwizardAppExtension<LedgerConfiguration> RULE = new DropwizardAppExtension<>(
//                LedgerApp.class, CONFIG_PATH
//        );

    @ClassRule
    public static final DropwizardAppRule<LedgerConfiguration> RULE = new DropwizardAppRule<>(
            LedgerApp.class, CONFIG_PATH
    );


//    @Before
//    public void setUp() {
//        RULE = new DropwizardAppExtension<>(
//                LedgerApp.class, CONFIG_PATH
//        );
//    }

    @Test
    public void shouldGetEventFromDB() {
        Client client = new JerseyClientBuilder(RULE.getEnvironment()).build("test client");
        String response = client.target("http://localhost:" + RULE.getLocalPort() + "/event/myevent")
                .request()
                .get(String.class);

        System.out.println(response);
    }
}
