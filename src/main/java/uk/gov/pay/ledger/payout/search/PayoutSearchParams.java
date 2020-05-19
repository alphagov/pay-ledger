package uk.gov.pay.ledger.payout.search;

import uk.gov.pay.ledger.common.search.SearchParams;
import uk.gov.pay.ledger.payout.state.PayoutState;
import uk.gov.pay.ledger.util.CommaDelimitedSetParameter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static uk.gov.pay.ledger.payout.state.PayoutState.from;

public class PayoutSearchParams extends SearchParams {

    private static final String GATEWAY_ACCOUNT_ID_FIELD = "gateway_account_id";
    private static final String STATE_FIELD = "state";
    private static final long DEFAULT_PAGE_NUMBER = 1L;
    private static final long DEFAULT_MAX_DISPLAY_SIZE = 500L;
    private static final long DEFAULT_DISPLAY_SIZE = 20L;

    private long maxDisplaySize = DEFAULT_MAX_DISPLAY_SIZE;

    private List<String> gatewayAccountIds;
    @QueryParam("payout_states")
    private CommaDelimitedSetParameter payoutStates;
    @DefaultValue("true")
    private Long pageNumber = 1L;
    private Long displaySize = DEFAULT_DISPLAY_SIZE;
    private Map<String, Object> queryMap;

    public void setGatewayAccountIds(List<String> gatewayAccountIds) {
        this.gatewayAccountIds = List.copyOf(gatewayAccountIds);
    }

    public void setPayoutStates(CommaDelimitedSetParameter payoutStates) {
        this.payoutStates = payoutStates;
    }

    @QueryParam("page")
    public void setPageNumber(Long pageNumber) {
        this.pageNumber = Objects.requireNonNullElse(pageNumber, DEFAULT_PAGE_NUMBER);
    }

    public PayoutSearchParams setDisplaySize(Long displaySize) {
        this.displaySize = displaySize;
        return this;
    }

    public List<String> getFilterTemplates() {
        List<String> filters = new ArrayList<>();

        if (gatewayAccountIds != null && !gatewayAccountIds.isEmpty()) {
            filters.add(" p.gateway_account_id IN (<" + GATEWAY_ACCOUNT_ID_FIELD + ">)");
        }

        if (isSet(payoutStates)) {
            filters.add(" (p.state IN (<" + STATE_FIELD + ">))");
        }
        return List.copyOf(filters);
    }

    public Map<String, Object> getQueryMap() {
        if (queryMap == null) {
            queryMap = new HashMap<>();

            if (gatewayAccountIds != null && !gatewayAccountIds.isEmpty()) {
                queryMap.put(GATEWAY_ACCOUNT_ID_FIELD, gatewayAccountIds);
            }

            if (isSet(payoutStates)) {
                queryMap.put(STATE_FIELD, getPayoutStatesForStatuses(payoutStates));
            }
        }
        return queryMap;
    }

    public List<String> getGatewayAccountIds() {
        return gatewayAccountIds;
    }

    public CommaDelimitedSetParameter getPayoutStates() {
        return payoutStates;
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

        if (isSet(payoutStates)) {
            queries.add(STATE_FIELD + "=" + payoutStates.getRawString());
        }

        queries.add("page=" + forPage);
        queries.add("display_size=" + getDisplaySize());

        return String.join("&", queries);
    }

    private List<String> getPayoutStatesForStatuses(CommaDelimitedSetParameter payoutStates) {
        return payoutStates.getParameters().stream()
                .map(status -> from(status))
                .map(PayoutState::name)
                .collect(Collectors.toList());
    }
}
