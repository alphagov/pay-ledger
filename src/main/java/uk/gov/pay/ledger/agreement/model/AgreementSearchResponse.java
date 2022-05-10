package uk.gov.pay.ledger.agreement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.ledger.util.pagination.PaginationBuilder;

import java.util.List;

public class AgreementSearchResponse {

    @JsonProperty("total")
    private Long total;
    @JsonProperty("count")
    private long count;
    @JsonProperty("page")
    private long page;
    @JsonProperty("results")
    List<Agreement> results;
    @JsonProperty("_links")
    private PaginationBuilder paginationBuilder;

    public AgreementSearchResponse(Long total, long count, long page,
                                   List<Agreement> results) {
        this.total = total;
        this.count = count;
        this.page = page;
        this.results = results;
    }

    public AgreementSearchResponse withPaginationBuilder(PaginationBuilder paginationBuilder) {
        this.paginationBuilder = paginationBuilder;
        return this;
    }

    public Long getTotal() {
        return total;
    }

    public long getCount() {
        return count;
    }

    public long getPage() {
        return page;
    }

    public List<Agreement> getResults() {
        return results;
    }

    public PaginationBuilder getPaginationBuilder() {
        return paginationBuilder;
    }
}