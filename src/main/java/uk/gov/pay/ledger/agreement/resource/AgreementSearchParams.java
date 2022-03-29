package uk.gov.pay.ledger.agreement.resource;

import uk.gov.pay.ledger.common.search.SearchParams;
import uk.gov.pay.ledger.payout.state.PayoutState;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class AgreementSearchParams extends SearchParams {

    private static final String SERCVICE_ID_FIELD = "service_id";
    private static final String LIVE_FIELD = "live";
    private static final String STATUS_FIELD = "status";
    private static final long DEFAULT_PAGE_NUMBER = 1L;
    private static final long DEFAULT_MAX_DISPLAY_SIZE = 20L;
    private static final long DEFAULT_DISPLAY_SIZE = 20L;

    private long maxDisplaySize = DEFAULT_MAX_DISPLAY_SIZE;

    private List<String> serviceIds;
    @QueryParam("status")
    private String status;
    @DefaultValue("true")
    private Long pageNumber = 1L;
    private Long displaySize = DEFAULT_DISPLAY_SIZE;
    private Map<String, Object> queryMap;

    public void setServiceIds(List<String> serviceIds) {
        this.serviceIds = List.copyOf(serviceIds);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @QueryParam("page")
    public void setPageNumber(Long pageNumber) {
        this.pageNumber = Objects.requireNonNullElse(pageNumber, DEFAULT_PAGE_NUMBER);
    }

    public List<String> getFilterTemplates() {
        List<String> filters = new ArrayList<>();

        if (serviceIds != null && !serviceIds.isEmpty()) {
            filters.add(" a.service_id IN (<" + SERCVICE_ID_FIELD + ">)");
        }

        if (isNotBlank(status)) {
            filters.add(" p.status = :" + STATUS_FIELD);
        }
        return List.copyOf(filters);
    }

    public Map<String, Object> getQueryMap() {
        if (queryMap == null) {
            queryMap = new HashMap<>();

            if (serviceIds != null && !serviceIds.isEmpty()) {
                queryMap.put(SERCVICE_ID_FIELD, serviceIds);
            }

            if (isNotBlank(status)) {
                queryMap.put(STATUS_FIELD,
                        PayoutState.fromState(status).name());
            }
        }
        return queryMap;
    }

    public List<String> getGatewayAccountIds() {
        return serviceIds;
    }

    public String getStatus() {
        return status;
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

        if (serviceIds != null && !serviceIds.isEmpty()) {
            queries.add(SERCVICE_ID_FIELD + "=" + String.join(",", serviceIds));
        }

        if (isNotBlank(status)) {
            queries.add(STATUS_FIELD + "=" + status);
        }

        queries.add("page=" + forPage);
        queries.add("display_size=" + getDisplaySize());

        return String.join("&", queries);
    }
}