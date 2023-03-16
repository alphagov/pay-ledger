package uk.gov.pay.ledger.event.resource;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.entity.EventEntity;
import uk.gov.pay.ledger.queue.EventMessageHandler;
import uk.gov.pay.ledger.util.fixture.EventFixture;

import javax.ws.rs.core.Response;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(DropwizardExtensionsSupport.class)
public class EventResourceTest {
    private static final EventDao dao = mock(EventDao.class);
    private static final EventMessageHandler eventMessageHandler = mock(EventMessageHandler.class);
    private static final Long eventId = 1L;
    private static final String nonExistentId = "I'm not really here";
    private final EventEntity event = EventFixture.anEventFixture()
            .withId(eventId)
            .toEntity();

    public static final ResourceExtension resources = ResourceExtension.builder()
            .addResource(new EventResource(dao, eventMessageHandler))
            .build();

    @BeforeEach
    public void setup() {
        when(dao.getById(eventId)).thenReturn(Optional.of(event));
    }

    @Test
    public void shouldReturn400IfFromAndToDatesNotSuppliedToTickerEndpoint() {
        Response response = resources.target("/v1/event/ticker").request().get();
        assertThat(response.getStatus(), is(400));
    }
}
