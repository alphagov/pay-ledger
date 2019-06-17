package uk.gov.pay.ledger.transaction.search.model;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PaginationBuilderTest {

    @Mock
    private UriInfo mockedUriInfo;

    TransactionSearchParams searchParams;
    private final String gatewayAccountExternalId = "a-gateway-account-external-id";

    @Before
    public void setUp() throws URISyntaxException {
        URI uri = new URI("http://example.org");
        when(mockedUriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(uri), UriBuilder.fromUri(uri));
        when(mockedUriInfo.getPath()).thenReturn("/transaction");

        searchParams = new TransactionSearchParams();
    }

    @Test
    public void shouldBuildLinksWithoutPrevAndNextLinks_whenTotalIsLessThanDisplaySize() {
        searchParams.setPageNumber(1L);
        searchParams.setDisplaySize(500L);

        PaginationBuilder builder = new PaginationBuilder(searchParams, mockedUriInfo)
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
        searchParams.setPageNumber(777L);
        searchParams.setDisplaySize(500L);

        PaginationBuilder builder = new PaginationBuilder(searchParams, mockedUriInfo)
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
        searchParams.setPageNumber(2L);
        searchParams.setDisplaySize(50L);
        PaginationBuilder builder = new PaginationBuilder(searchParams, mockedUriInfo)
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
        searchParams.setPageNumber(3L);
        searchParams.setDisplaySize(10L);
        PaginationBuilder builder = new PaginationBuilder(searchParams, mockedUriInfo)
                .withTotalCount(120L);
        builder = builder.buildResponse();
        assertThat(builder.getFirstLink().getHref().contains("page=1&display_size=10"), is(true));
        assertThat(builder.getLastLink().getHref().contains("page=12&display_size=10"), is(true));
        assertThat(builder.getPrevLink().getHref().contains("page=2&display_size=10"), is(true));
        assertThat(builder.getNextLink().getHref().contains("page=4&display_size=10"), is(true));
        assertThat(builder.getSelfLink().getHref().contains("page=3&display_size=10"), is(true));
    }
}