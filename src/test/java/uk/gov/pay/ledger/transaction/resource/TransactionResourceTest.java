package uk.gov.pay.ledger.transaction.resource;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.exception.BadRequestExceptionMapper;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
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
        when(mockTransactionService.getTransaction(eq("some-external-id"), anyInt())).thenReturn(Optional.of(new TransactionView()));

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

    @Test
    public void shouldReturn400IfTransactionTypeIsNotCorrectForSearch() {
        Response response = resources
                .target("/v1/transaction")
                .queryParam("account_id", "666")
                .queryParam("transaction_type", "not_existing_transaction_type")
                .request()
                .get();

        Map responseMessage = response.readEntity(new GenericType<HashMap>() {
        });
        assertThat(response.getStatus(), is(400));
        assertThat(responseMessage.get("message"), is("query param transaction_type must be one of [PAYMENT, REFUND]"));
    }

    @Test
    public void findTransactionsForTransactionShouldReturn400IfGatewayAccountIdIsNotProvided() {
        Response response = resources
                .target("/v1/transaction/parent-transaction-id/transaction")
                .request()
                .get();

        Map responseMessage = response.readEntity(new GenericType<HashMap>() {
        });
        List errors = (List) responseMessage.get("errors");

        assertThat(response.getStatus(), is(400));
        assertThat(errors.get(0), is("query param gateway_account_id may not be empty"));
    }
}
