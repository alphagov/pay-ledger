package uk.gov.pay.ledger.payout.search;

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

public class PayoutSearchParams {

    private static final String GATEWAY_ACCOUNT_ID_FIELD = "gateway_account_id";
    private static final String STATE_FIELD = "state";
    private static final long DEFAULT_PAGE_NUMBER = 1L;
    private static final long DEFAULT_MAX_DISPLAY_SIZE = 500L;

    private long maxDisplaySize = DEFAULT_MAX_DISPLAY_SIZE;

    private List<String> accountIds;
    @QueryParam("payout_states")
    private CommaDelimitedSetParameter payoutStates;
    @DefaultValue("true")
    private Long pageNumber = 1L;
    private Long displaySize = DEFAULT_MAX_DISPLAY_SIZE;
    private Map<String, Object> queryMap;

    public void setAccountIds(List<String> accountIds) {
        this.accountIds = List.copyOf(accountIds);
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

        if (accountIds != null && !accountIds.isEmpty()) {
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

            if (accountIds != null && !accountIds.isEmpty()) {
                queryMap.put(GATEWAY_ACCOUNT_ID_FIELD, accountIds);
            }

            if (isSet(payoutStates)) {
                queryMap.put(STATE_FIELD, getPayoutStatesForStatuses(payoutStates));
            }
        }
        return queryMap;
    }

    public List<String> getAccountIds() {
        return accountIds;
    }

    public CommaDelimitedSetParameter getPayoutStates() {
        return payoutStates;
    }

    public Long getPageNumber() {
        return pageNumber;
    }

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

    private boolean isSet(CommaDelimitedSetParameter commaDelimitedSetParameter) {
        return commaDelimitedSetParameter != null && commaDelimitedSetParameter.isNotEmpty();
    }

    private List<String> getPayoutStatesForStatuses(CommaDelimitedSetParameter payoutStates) {
        return payoutStates.getParameters().stream()
                .map(status -> from(status))
                .map(PayoutState::name)
                .collect(Collectors.toList());
    }
}
