package uk.gov.pay.ledger.payout.search;

import io.swagger.v3.oas.annotations.Parameter;
import uk.gov.pay.ledger.common.search.SearchParams;
import uk.gov.pay.ledger.payout.state.PayoutState;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class PayoutSearchParams extends SearchParams {

    private static final String GATEWAY_ACCOUNT_ID_FIELD = "gateway_account_id";
    private static final String STATE_FIELD = "state";
    private static final long DEFAULT_PAGE_NUMBER = 1L;
    private static final long DEFAULT_MAX_DISPLAY_SIZE = 500L;
    private static final long DEFAULT_DISPLAY_SIZE = 20L;

    private long maxDisplaySize = DEFAULT_MAX_DISPLAY_SIZE;

    private List<String> gatewayAccountIds;
    @QueryParam("state")
    @Parameter(description = "State of payout. allowed values are intransit, paidout, failed")
    private String state;
    @DefaultValue("true")
    private Long pageNumber = 1L;
    private Long displaySize = DEFAULT_DISPLAY_SIZE;
    private Map<String, Object> queryMap;

    public void setGatewayAccountIds(List<String> gatewayAccountIds) {
        this.gatewayAccountIds = List.copyOf(gatewayAccountIds);
    }

    public void setState(String state) {
        this.state = state;
    }

    @QueryParam("page")
    @Parameter(description = "Page number requested for the search, should be a positive integer (optional, defaults to 1)")
    public void setPageNumber(Long pageNumber) {
        this.pageNumber = Objects.requireNonNullElse(pageNumber, DEFAULT_PAGE_NUMBER);
    }

    @QueryParam("display_size")
    @Parameter(description = "Number of results to be shown per page, should be a positive integer (optional, defaults to 500, max 500)")
    public PayoutSearchParams setDisplaySize(Long displaySize) {
        this.displaySize = Objects.requireNonNullElse(displaySize, DEFAULT_DISPLAY_SIZE);
        return this;
    }

    public List<String> getFilterTemplates() {
        List<String> filters = new ArrayList<>();

        if (gatewayAccountIds != null && !gatewayAccountIds.isEmpty()) {
            filters.add(" p.gateway_account_id IN (<" + GATEWAY_ACCOUNT_ID_FIELD + ">)");
        }

        if (isNotBlank(state)) {
            filters.add(" p.state = :" + STATE_FIELD);
        }
        return List.copyOf(filters);
    }

    public Map<String, Object> getQueryMap() {
        if (queryMap == null) {
            queryMap = new HashMap<>();

            if (gatewayAccountIds != null && !gatewayAccountIds.isEmpty()) {
                queryMap.put(GATEWAY_ACCOUNT_ID_FIELD, gatewayAccountIds);
            }

            if (isNotBlank(state)) {
                queryMap.put(STATE_FIELD,
                        PayoutState.fromState(state).name());
            }
        }
        return queryMap;
    }

    public List<String> getGatewayAccountIds() {
        return gatewayAccountIds;
    }

    public String getState() {
        return state;
    }

    @Override
    public Long getPageNumber() {
        return pageNumber;
    }

    @Override
    public Long getDisplaySize() {
        if (this.displaySize > this.maxDisplaySize) {
            return this.maxDisplaySize;
        } else {
            return this.displaySize;
        }
    }

    public Long getOffset() {
        long offset = 0L;

        if (pageNumber != null) {
            offset = (pageNumber - 1) * getDisplaySize();
        }

        return offset;
    }

    @Override
    public String buildQueryParamString(Long forPage) {
        List<String> queries = new ArrayList<>();

        if (gatewayAccountIds != null && !gatewayAccountIds.isEmpty()) {
            queries.add(GATEWAY_ACCOUNT_ID_FIELD + "=" + String.join(",", gatewayAccountIds));
        }

        if (isNotBlank(state)) {
            queries.add(STATE_FIELD + "=" + state);
        }

        queries.add("page=" + forPage);
        queries.add("display_size=" + getDisplaySize());

        return String.join("&", queries);
    }
}
