package uk.gov.pay.ledger.util.pagination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.payout.search.PayoutSearchParams;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
public class PaginationBuilderTest {

    @Mock
    private UriInfo mockedUriInfo;

    TransactionSearchParams transactionSearchParams;
    PayoutSearchParams payoutSearchParams;
    private final String gatewayAccountExternalId = "a-gateway-account-external-id";

    @BeforeEach
    public void setUp() throws URISyntaxException {
        URI uri = new URI("http://example.org");
        Mockito.when(mockedUriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(uri), UriBuilder.fromUri(uri));
        Mockito.when(mockedUriInfo.getPath()).thenReturn("/transaction");

        transactionSearchParams = new TransactionSearchParams();
        payoutSearchParams = new PayoutSearchParams();
    }

    @Test
    public void shouldBuildLinksWithoutPrevAndNextLinks_whenTotalIsLessThanDisplaySize() {
        transactionSearchParams.setPageNumber(1L);
        transactionSearchParams.setDisplaySize(500L);

        PaginationBuilder builder = new PaginationBuilder(transactionSearchParams, mockedUriInfo)
                .withTotalCount(120L);
        builder = builder.buildResponse();
        assertThat(builder.getPrevLink(), is(nullValue()));
        assertThat(builder.getFirstLink().getHref().contains("page=1&display_size=500"), is(true));
        assertThat(builder.getLastLink().getHref().contains("page=1&display_size=500"), is(true));
        assertThat(builder.getSelfLink().getHref().contains("page=1&display_size=500"), is(true));
        assertThat(builder.getNextLink(), is(nullValue()));
    }

    @Test
    public void shouldBuildPrevFirstLastLinksWithPageSetTo1_whenRequestedPageIsGreaterThanLastPage() {
        payoutSearchParams.setPageNumber(777L);
        payoutSearchParams.setDisplaySize(500L);

        PaginationBuilder builder = new PaginationBuilder(payoutSearchParams, mockedUriInfo)
                .withTotalCount(120L);
        builder = builder.buildResponse();
        assertThat(builder.getPrevLink().getHref().contains("page=1&display_size=500"), is(true));
        assertThat(builder.getFirstLink().getHref().contains("page=1&display_size=500"), is(true));
        assertThat(builder.getLastLink().getHref().contains("page=1&display_size=500"), is(true));
        assertThat(builder.getSelfLink().getHref().contains("page=777&display_size=500"), is(true));
        assertThat(builder.getNextLink(), is(nullValue()));
    }

    @Test
    public void shouldShowPrevAndFirstLinkAsEqualsAndLastAndNextLinkAsEquals() {
        transactionSearchParams.setPageNumber(2L);
        transactionSearchParams.setDisplaySize(50L);
        PaginationBuilder builder = new PaginationBuilder(transactionSearchParams, mockedUriInfo)
                .withTotalCount(120L);
        builder = builder.buildResponse();
        assertThat(builder.getPrevLink().getHref().contains("page=1&display_size=50"), is(true));
        assertThat(builder.getFirstLink().getHref().contains("page=1&display_size=50"), is(true));
        assertThat(builder.getLastLink().getHref().contains("page=3&display_size=50"), is(true));
        assertThat(builder.getNextLink().getHref().contains("page=3&display_size=50"), is(true));
        assertThat(builder.getSelfLink().getHref().contains("page=2&display_size=50"), is(true));
    }

    @Test
    public void shouldShowAllLinksCorrectly_whenMultiplePagesExists() {
        payoutSearchParams.setPageNumber(3L);
        payoutSearchParams.setDisplaySize(10L);
        PaginationBuilder builder = new PaginationBuilder(payoutSearchParams, mockedUriInfo)
                .withTotalCount(120L);
        builder = builder.buildResponse();
        assertThat(builder.getFirstLink().getHref().contains("page=1&display_size=10"), is(true));
        assertThat(builder.getLastLink().getHref().contains("page=12&display_size=10"), is(true));
        assertThat(builder.getPrevLink().getHref().contains("page=2&display_size=10"), is(true));
        assertThat(builder.getNextLink().getHref().contains("page=4&display_size=10"), is(true));
        assertThat(builder.getSelfLink().getHref().contains("page=3&display_size=10"), is(true));
    }

    @Test
    public void shouldBuildLinksCorrectly_whenLimitTotalParamIsSetAndFirstPageIsAccessed() {
        transactionSearchParams.setPageNumber(1L);
        transactionSearchParams.setDisplaySize(10L);
        transactionSearchParams.setLimitTotal(true);
        PaginationBuilder builder = new PaginationBuilder(transactionSearchParams, mockedUriInfo)
                .withTotalCount(120L)
                .withCount(10L);
        builder = builder.buildResponse();
        assertThat(builder.getFirstLink().getHref().contains("page=1&display_size=10"), is(true));
        assertThat(builder.getLastLink(), is(nullValue()));
        assertThat(builder.getPrevLink(), is(nullValue()));
        assertThat(builder.getNextLink().getHref().contains("page=2&display_size=10"), is(true));
        assertThat(builder.getSelfLink().getHref().contains("page=1&display_size=10"), is(true));
    }

    @Test
    public void shouldShowAllLinksCorrectly_whenLimitTotalParamIsSetAndMultiplePagesExists() {
        transactionSearchParams.setPageNumber(3L);
        transactionSearchParams.setDisplaySize(10L);
        transactionSearchParams.setLimitTotal(true);
        PaginationBuilder builder = new PaginationBuilder(transactionSearchParams, mockedUriInfo)
                .withTotalCount(120L)
                .withCount(10L);
        builder = builder.buildResponse();
        assertThat(builder.getFirstLink().getHref().contains("page=1&display_size=10"), is(true));
        assertThat(builder.getLastLink(), is(nullValue()));
        assertThat(builder.getPrevLink().getHref().contains("page=2&display_size=10"), is(true));
        assertThat(builder.getNextLink().getHref().contains("page=4&display_size=10"), is(true));
        assertThat(builder.getSelfLink().getHref().contains("page=3&display_size=10"), is(true));
    }

    @Test
    public void shouldNotGenerateNextLink_whenLimitTotalIsSetAndCountIsLessThanPageSize() {
        transactionSearchParams.setDisplaySize(10L);
        transactionSearchParams.setLimitTotal(true);
        PaginationBuilder builder = new PaginationBuilder(transactionSearchParams, mockedUriInfo)
                .withTotalCount(120L)
                .withCount(9L);
        builder = builder.buildResponse();
        assertThat(builder.getNextLink(), is(nullValue()));
    }
    
    @Test
    public void shouldGenerateSelfLinkWithOnlyPrescribedSpecialCharactersURLEncoded(){
        Map<String, Object> valuesForReferenceParameter = Map.of(
                "{ref", "reference=%7Bref",
                "[ref", "reference=%5Bref",
                "r{e{f}{", "reference=r%7Be%7Bf%7D%7B",
                "ref=", "reference=ref=",
                "ref&", "reference=ref&",
                "ref@", "reference=ref%40"
        );
        for (Map.Entry<String, Object> entry : valuesForReferenceParameter.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            transactionSearchParams.setReference(key);
            PaginationBuilder builder = new PaginationBuilder(transactionSearchParams, mockedUriInfo)
                    .withTotalCount(120L)
                    .withCount(9L)
                    .buildResponse();
            assertThat(builder.getSelfLink().toString().contains(value.toString()), is(true));
            assertDoesNotThrow(builder::getSelfLink);
        }
    }
}
