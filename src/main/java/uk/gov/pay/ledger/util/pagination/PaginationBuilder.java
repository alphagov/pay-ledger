package uk.gov.pay.ledger.util.pagination;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.ledger.common.search.SearchParams;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class PaginationBuilder {

    private static final String SELF_LINK = "self";
    private static final String FIRST_LINK = "first_page";
    private static final String LAST_LINK = "last_page";
    private static final String PREV_LINK = "prev_page";
    private static final String NEXT_LINK = "next_page";
    private SearchParams searchParams;
    private UriInfo uriInfo;

    @JsonIgnore
    private Long totalCount;
    @JsonIgnore
    private Long selfPageNum;
    @JsonProperty(SELF_LINK)
    private PaginationLink selfLink;
    @JsonProperty(FIRST_LINK)
    private PaginationLink firstLink;
    @JsonProperty(LAST_LINK)
    private PaginationLink lastLink;
    @JsonProperty(PREV_LINK)
    private PaginationLink prevLink;
    @JsonProperty(NEXT_LINK)
    private PaginationLink nextLink;

    public PaginationBuilder(SearchParams searchParams, UriInfo uriInfo) {
        this.searchParams = searchParams;
        this.uriInfo = uriInfo;
        selfPageNum = searchParams.getPageNumber();
    }

    public PaginationBuilder withTotalCount(Long total) {
        this.totalCount = total;
        return this;
    }

    public PaginationBuilder buildResponse() {

        if (searchParams.limitTotal()) {
            buildLinksForLimitTotal();
        } else {
            Long pageSize = searchParams.getDisplaySize();
            long lastPage = totalCount > 0 ? (totalCount + pageSize - 1) / pageSize : 1;
            buildLinks(lastPage);
        }

        return this;
    }

    public PaginationLink getSelfLink() {
        return selfLink;
    }

    public PaginationLink getFirstLink() {
        return firstLink;
    }

    public PaginationLink getLastLink() {
        return lastLink;
    }

    public PaginationLink getPrevLink() {
        return prevLink;
    }

    public PaginationLink getNextLink() {
        return nextLink;
    }

    private void buildLinksForLimitTotal() {
        selfLink = PaginationLink.ofValue(uriWithParams(searchParams.buildQueryParamString(searchParams.getPageNumber())));
        firstLink = PaginationLink.ofValue(uriWithParams(searchParams.buildQueryParamString(1L)));

        nextLink = PaginationLink.ofValue(uriWithParams(searchParams.buildQueryParamString(selfPageNum + 1)));

        if (selfPageNum == 1L) {
            prevLink = null;
        } else {
            prevLink = PaginationLink.ofValue(uriWithParams(searchParams.buildQueryParamString(selfPageNum - 1)));
        }
    }

    private void buildLinks(long lastPage) {
        selfLink = PaginationLink.ofValue(uriWithParams(searchParams.buildQueryParamString(searchParams.getPageNumber())));
        firstLink = PaginationLink.ofValue(uriWithParams(searchParams.buildQueryParamString(1L)));
        lastLink = PaginationLink.ofValue(uriWithParams(searchParams.buildQueryParamString(lastPage)));
        nextLink = (selfPageNum >= lastPage) ? null : PaginationLink.ofValue(
                uriWithParams(searchParams.buildQueryParamString(selfPageNum + 1)));

        if (selfPageNum == 1L) {
            prevLink = null;
        } else {
            if (selfPageNum > lastPage) {
                prevLink = PaginationLink.ofValue(uriWithParams(searchParams.buildQueryParamString(lastPage)));
            } else {
                prevLink = PaginationLink.ofValue(uriWithParams(searchParams.buildQueryParamString(selfPageNum - 1)));
            }
        }
    }

    private String uriWithParams(String queryParams) {
        URI uri = uriInfo.getBaseUriBuilder()
                .replacePath(uriInfo.getPath())
                .replaceQuery(queryParams)
                .build();

        return uri.toString();
    }
}
