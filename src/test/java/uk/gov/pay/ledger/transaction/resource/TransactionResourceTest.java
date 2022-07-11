package uk.gov.pay.ledger.transaction.resource;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.exception.BadRequestExceptionMapper;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.transaction.service.CsvService;
import uk.gov.pay.ledger.transaction.service.TransactionService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(MockitoExtension.class)
public class TransactionResourceTest {
    private static final TransactionService mockTransactionService = mock(TransactionService.class);
    private static final CsvService mockCsvService = mock(CsvService.class);
    private static final LedgerConfig mockConfig = mock(LedgerConfig.class);

    public static final ResourceExtension resources = ResourceExtension.builder()
            .addResource(new TransactionResource(mockTransactionService, mockCsvService, mockConfig))
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
    public void shouldReturn404IfTransactionDoesNotExistWhenGettingEvents() {
        when(mockTransactionService.findTransactionEvents(any(), any(), anyBoolean(), anyInt()))
                .thenThrow(new WebApplicationException("Not found", Response.Status.NOT_FOUND));

        Response response = resources
                .target("/v1/transaction/non-existent-id/event")
                .queryParam("gateway_account_id", 1)
                .request()
                .get();

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void shouldReturn400IfTransactionGatewayAccountIdIsNotProvidedForSearch() {
        Response response = resources.target("/v1/transaction/")
                .request()
                .header("Accept", APPLICATION_JSON)
                .get();

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

        Map responseMessage = response.readEntity(new GenericType<HashMap>() {});
        assertThat(response.getStatus(), is(400));
        assertThat(responseMessage.get("message"), is("query param transaction_type must be one of [PAYMENT, REFUND, DISPUTE]"));
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
        assertThat(errors.get(0), is("query param gateway_account_id must not be empty"));
    }

    @Test
    public void findByGatewayTransactionId_ShouldReturn404IfTransactionNotFound() {
        when(mockTransactionService.findByGatewayTransactionId(anyString(), anyString()))
                .thenReturn(Optional.empty());

        Response response = resources
                .target("/v1/transaction/gateway-transaction/example-gateway-transaction-id")
                .queryParam("payment_provider", "sandbox")
                .request()
                .get();

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void findByGatewayTransactionId_ShouldReturn400IfPaymentProviderQueryParamIsEmpty() {
        Response response = resources
                .target("/v1/transaction/gateway-transaction/exampleGatewayTransactionId")
                .queryParam("transaction_type", "PAYMENT")
                .request()
                .get();

        Map responseMessage = response.readEntity(new GenericType<HashMap>() {
        });
        List errors = (List) responseMessage.get("errors");

        assertThat(response.getStatus(), is(400));
        assertThat(errors.get(0), is("query param payment_provider must not be empty"));
    }

    @Test
    public void searchTransactionForCsvShouldReturn400IfGatewayAccountIdIsNotAvailable() {
        Response response = resources
                .target("/v1/transaction")
                .request()
                .accept("text/csv")
                .get();

        HashMap<String, Object> responseMessage = response.readEntity(HashMap.class);

        assertThat(response.getStatus(), is(400));
        assertThat(responseMessage.get("error_identifier"), is("GENERIC"));
        assertThat((List<String>) responseMessage.get("message"),
                Matchers.containsInAnyOrder("gateway_account_id is mandatory to search transactions for CSV"));
    }
}
