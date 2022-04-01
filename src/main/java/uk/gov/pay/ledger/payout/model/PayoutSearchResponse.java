package uk.gov.pay.ledger.payout.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.ledger.util.pagination.PaginationBuilder;

import java.util.List;

public class PayoutSearchResponse {

    @JsonProperty("total")
    @Schema(example = "1000")
    private Long total;
    @JsonProperty("count")
    @Schema(example = "100")
    private long count;
    @JsonProperty("page")
    @Schema(example = "1")
    private long page;
    @JsonProperty("results")
    @ArraySchema(schema = @Schema(implementation = PayoutView.class))
    List<PayoutView> payoutViewList;
    @JsonProperty("_links")
    private PaginationBuilder paginationBuilder;

    public PayoutSearchResponse(Long total, long count, long page,
                                List<PayoutView> payoutViewList) {
        this.total = total;
        this.count = count;
        this.page = page;
        this.payoutViewList = payoutViewList;
    }

    public PayoutSearchResponse withPaginationBuilder(PaginationBuilder paginationBuilder) {
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

    public List<PayoutView> getPayoutViewList() {
        return payoutViewList;
    }

    public PaginationBuilder getPaginationBuilder() {
        return paginationBuilder;
    }
}
