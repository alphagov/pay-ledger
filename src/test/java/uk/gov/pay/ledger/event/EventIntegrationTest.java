package uk.gov.pay.ledger.event;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.pay.ledger.rules.AppWithPostgresRule;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Ignore
public class EventIntegrationTest {
    @ClassRule
    public static AppWithPostgresRule rule = new AppWithPostgresRule();

    private Client client = rule.getAppRule().client();
    private Integer port = rule.getAppRule().getLocalPort();

    @Test
    public void shouldGetEventFromDB() {
        Response response = client.target("http://localhost:" + port + "/v1/event/myevent")
                .request()
                .get();

        // Asserting 404 as haven't decided how to do db fixtures yet. Getting a 404 is a good start
        assertThat(response.getStatus(), is(404));
    }
}
