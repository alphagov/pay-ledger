package uk.gov.pay.ledger.transaction;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.resources.TransactionResource;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class TransactionResourceTest {
    private static final TransactionDao dao = mock(TransactionDao.class);

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new TransactionResource(dao))
            .build();

    @Test
    public void shouldReturn404IfTransactionDoesNotExist() {
        Response response = resources.target("/v1/transaction/non-existent-id").request().get();
        assertThat(response.getStatus(), is(404));
    }
}
