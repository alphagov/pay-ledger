package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.ledger.transaction.search.model.PaginationBuilder;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;

import java.util.List;

public class TransactionSearchResponse {

    @JsonIgnore
    private String gatewayExternalId;
    @JsonProperty("total")
    private Long total;
    @JsonProperty("count")
    private long count;
    @JsonProperty("page")
    private long page;
    @JsonProperty("results")
    List<TransactionView> transactionViewList;
    @JsonProperty("_links")
    private PaginationBuilder paginationBuilder;

    public TransactionSearchResponse(String gatewayExternalId, Long total, Long count, Long page,
                                     List<TransactionView> transactionViewList) {
        this.gatewayExternalId = gatewayExternalId;
        this.total = total;
        this.count = count;
        this.page = page;
        this.transactionViewList = transactionViewList;
    }

    public TransactionSearchResponse withPaginationBuilder(PaginationBuilder paginationBuilder) {
        this.paginationBuilder = paginationBuilder;
        return this;
    }

    public String getGatewayExternalId() {
        return gatewayExternalId;
    }

    public Long getTotal() {
        return total;
    }

    public Long getCount() {
        return count;
    }

    public Long getPage() {
        return page;
    }

    public List<TransactionView> getTransactionViewList() {
        return transactionViewList;
    }

    public PaginationBuilder getPaginationBuilder() {
        return paginationBuilder;
    }
}
