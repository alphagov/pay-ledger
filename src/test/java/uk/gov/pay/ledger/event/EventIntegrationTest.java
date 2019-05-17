package uk.gov.pay.ledger.event;

import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.rules.AppWithPostgresRule;

import javax.ws.rs.client.Client;

public class EventIntegrationTest {
    @ClassRule
    public static AppWithPostgresRule rule = new AppWithPostgresRule();

    private Client client = rule.getAppRule().client();
    private Integer port = rule.getAppRule().getLocalPort();

    @Test
    public void shouldGetEventFromDB() {
        String response = client.target("http://localhost:" + port + "/event/myevent")
                .request()
                .get(String.class);

        System.out.println(response);
    }
}
