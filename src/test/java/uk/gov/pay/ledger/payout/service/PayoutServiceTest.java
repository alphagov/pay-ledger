package uk.gov.pay.ledger.payout.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.payout.dao.PayoutDao;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.payout.model.PayoutEntityFactory;
import uk.gov.pay.ledger.payout.model.PayoutSearchResponse;
import uk.gov.pay.ledger.payout.search.PayoutSearchParams;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.util.fixture.PayoutFixture.aPayoutList;

@ExtendWith(MockitoExtension.class)
public class PayoutServiceTest {

    @Mock
    private PayoutDao mockPayoutDao;
    @Mock
    private PayoutEntityFactory payoutEntityFactory;
    @Mock
    private UriInfo mockUriInfo;
    private PayoutService payoutService;
    private String gatewayAccountId = "12345";
    private PayoutSearchParams searchParams;

    @BeforeEach
    public void setUp() {
        payoutService = new PayoutService(mockPayoutDao, payoutEntityFactory);
        lenient().when(mockUriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri("http://example.com"));
        lenient().when(mockUriInfo.getPath()).thenReturn("/v1/payout");
    }

    @Test
    public void shouldReturnAPaginatedPayoutSearchResponse() {
        List<String> gatewayAccountIds = List.of(gatewayAccountId);
        searchParams = new PayoutSearchParams();
        searchParams.setPageNumber(1L);

        List<PayoutEntity> entityList = aPayoutList(gatewayAccountId, 10);
        when(mockPayoutDao.searchPayouts(searchParams)).thenReturn(entityList);
        when(mockPayoutDao.getTotalForSearch(searchParams)).thenReturn(10L);

        PayoutSearchResponse response = payoutService.searchPayouts(gatewayAccountIds, searchParams, mockUriInfo);
        assertThat(response.getTotal(), is(10L));
        assertThat(response.getCount(), is(10L));
        assertThat(response.getPage(), is(1L));
        assertThat(response.getPayoutViewList().size(), is(10));
        assertThat(response.getPayoutViewList().get(2).getGatewayAccountId(), is(gatewayAccountId));
        assertThat(response.getPaginationBuilder().getSelfLink().getHref(), is("http://example.com/v1/payout?gateway_account_id=12345&page=1&display_size=20"));
        assertThat(response.getPaginationBuilder().getFirstLink().getHref(), is("http://example.com/v1/payout?gateway_account_id=12345&page=1&display_size=20"));
        assertThat(response.getPaginationBuilder().getLastLink().getHref(), is("http://example.com/v1/payout?gateway_account_id=12345&page=1&display_size=20"));
        assertThat(response.getPaginationBuilder().getLastLink().getHref(), is("http://example.com/v1/payout?gateway_account_id=12345&page=1&display_size=20"));
        assertThat(response.getPaginationBuilder().getNextLink(), is(nullValue()));
    }

    @Test
    public void shouldThrowException_whenInvalidPageNumberIsRequested() {
        searchParams = new PayoutSearchParams();
        searchParams.setPageNumber(2L);
        searchParams.setGatewayAccountIds(List.of(gatewayAccountId));
        when(mockPayoutDao.searchPayouts(searchParams)).thenReturn(List.of());
        when(mockPayoutDao.getTotalForSearch(searchParams)).thenReturn(10L);

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
                () -> payoutService.searchPayouts(searchParams, mockUriInfo));
        assertThat(webApplicationException.getMessage(), is("The requested page was not found"));
    }

    @Test
    public void shouldReturnAPaginatedPayoutSearchResponse_withNextLink() {
        List<String> gatewayAccountIds = List.of(gatewayAccountId);
        searchParams = new PayoutSearchParams();
        searchParams.setPageNumber(1L);
        searchParams.setDisplaySize(5L);

        List<PayoutEntity> entityList = aPayoutList(gatewayAccountId, 10);
        when(mockPayoutDao.searchPayouts(searchParams)).thenReturn(entityList);
        when(mockPayoutDao.getTotalForSearch(searchParams)).thenReturn(10L);

        PayoutSearchResponse response = payoutService.searchPayouts(gatewayAccountIds, searchParams, mockUriInfo);

        assertThat(response.getTotal(), is(10L));
        assertThat(response.getCount(), is(10L));
        assertThat(response.getPage(), is(1L));
        assertThat(response.getPayoutViewList().size(), is(10));
        assertThat(response.getPayoutViewList().get(2).getGatewayAccountId(), is(gatewayAccountId));
        assertThat(response.getPaginationBuilder().getSelfLink().getHref(), is("http://example.com/v1/payout?gateway_account_id=12345&page=1&display_size=5"));
        assertThat(response.getPaginationBuilder().getFirstLink().getHref(), is("http://example.com/v1/payout?gateway_account_id=12345&page=1&display_size=5"));
        assertThat(response.getPaginationBuilder().getLastLink().getHref(), is("http://example.com/v1/payout?gateway_account_id=12345&page=2&display_size=5"));
        assertThat(response.getPaginationBuilder().getLastLink().getHref(), is("http://example.com/v1/payout?gateway_account_id=12345&page=2&display_size=5"));
        assertThat(response.getPaginationBuilder().getNextLink().getHref(), is("http://example.com/v1/payout?gateway_account_id=12345&page=2&display_size=5"));
    }

    @Test
    public void shouldReturnAPaginatedPayoutSearchResponse_withMultipleGatewayAccounts() {
        String gatewayAccountId2 = "12346";
        List<String> gatewayAccountIds = List.of(gatewayAccountId, gatewayAccountId2);
        searchParams = new PayoutSearchParams();
        searchParams.setPageNumber(1L);
        searchParams.setDisplaySize(14L);

        List<PayoutEntity> entityList = aPayoutList(gatewayAccountId, 10);
        entityList.addAll(aPayoutList(gatewayAccountId2, 5));
        when(mockPayoutDao.searchPayouts(searchParams)).thenReturn(entityList);
        when(mockPayoutDao.getTotalForSearch(searchParams)).thenReturn(15L);
        PayoutSearchResponse response = payoutService.searchPayouts(gatewayAccountIds, searchParams, mockUriInfo);

        assertThat(response.getPayoutViewList().size(), is(15));

        assertThat(response.getPayoutViewList().get(2).getGatewayAccountId(), is(gatewayAccountId));
        assertThat(response.getPaginationBuilder().getSelfLink().getHref(), is("http://example.com/v1/payout?gateway_account_id=12345%2C12346&page=1&display_size=14"));
        assertThat(response.getPaginationBuilder().getFirstLink().getHref(), is("http://example.com/v1/payout?gateway_account_id=12345%2C12346&page=1&display_size=14"));
        assertThat(response.getPaginationBuilder().getLastLink().getHref(), is("http://example.com/v1/payout?gateway_account_id=12345%2C12346&page=2&display_size=14"));
        assertThat(response.getPaginationBuilder().getLastLink().getHref(), is("http://example.com/v1/payout?gateway_account_id=12345%2C12346&page=2&display_size=14"));
        assertThat(response.getPaginationBuilder().getNextLink().getHref(), is("http://example.com/v1/payout?gateway_account_id=12345%2C12346&page=2&display_size=14"));
    }
}