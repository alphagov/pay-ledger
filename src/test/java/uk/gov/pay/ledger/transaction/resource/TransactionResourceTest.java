package uk.gov.pay.ledger.transaction.resource;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.exception.BadRequestExceptionMapper;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import javax.ws.rs.core.Response;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransactionResourceTest {
    private static final TransactionService mockTransactionService = mock(TransactionService.class);

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new TransactionResource(mockTransactionService))
            .addProvider(BadRequestExceptionMapper.class)
            .build();

    @Test
    public void shouldReturn400IfTransactionGatewayAccountIdIsNotProvided() {
        Response response = resources
                .target("/v1/transaction/non-existent-id")
                .request()
                .get();

        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void shouldReturn200IfTransactionGatewayAccountIdIsNotProvidedButNotRequiredFlag() {
        when(mockTransactionService.getTransaction(eq("some-external-id"), any())).thenReturn(Optional.of(new TransactionView()));

        Response response = resources
                .target("/v1/transaction/some-external-id")
                .queryParam("override_account_id_restriction", true)
                .request()
                .get();

        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void shouldReturn404IfTransactionDoesNotExist() {
        Response response = resources
                .target("/v1/transaction/non-existent-id")
                .queryParam("account_id", 1)
                .request()
                .get();

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void shouldReturn400IfTransactionGatewayAccountIdIsNotProvidedForSearch() {
        Response response = resources.target("/v1/transaction/").request().get();
        assertThat(response.getStatus(), is(400));
    }
}
