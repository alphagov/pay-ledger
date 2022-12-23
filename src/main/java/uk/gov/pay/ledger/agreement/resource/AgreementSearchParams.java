package uk.gov.pay.ledger.agreement.resource;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.ledger.common.search.SearchParams;
import uk.gov.service.payments.commons.model.agreement.AgreementStatus;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class AgreementSearchParams extends SearchParams {

    private static final String SERVICE_ID_FIELD = "service_id";
    private static final String GATEWAY_ACCOUNT_ID_FIELD = "account_id";
    private static final String LIVE_FIELD = "live";
    private static final String STATUS_FIELD = "status";
    private static final String REFERENCE_FIELD = "reference";
    private static final long DEFAULT_PAGE_NUMBER = 1L;
    private static final long DEFAULT_MAX_DISPLAY_SIZE = 20L;
    public static final long DEFAULT_DISPLAY_SIZE = 20L;

    private long maxDisplaySize = DEFAULT_MAX_DISPLAY_SIZE;

    @NotNull
    @QueryParam("service_id")
    private List<String> serviceIds;
    @QueryParam("live")
    private Boolean live;
    @QueryParam("account_id")
    private List<String> gatewayAccountIds;
    @QueryParam("status")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
    private AgreementStatus status;
    @QueryParam("exact_reference_match")
    private Boolean exactReferenceMatch;
    @QueryParam("reference")
    private String reference;
    private Long pageNumber = 1L;

    private Long displaySize = DEFAULT_DISPLAY_SIZE;
    private Map<String, Object> queryMap;

    @AssertTrue(message = "One of field [" + SERVICE_ID_FIELD + "] or field [" + GATEWAY_ACCOUNT_ID_FIELD + "] are required")
    private boolean isServiceIdsOrGatewayAccountIds() {
        return !(getServiceIds().isEmpty() && getGatewayAccountIds().isEmpty());
    }

    public void setServiceIds(List<String> serviceIds) {
        this.serviceIds = List.copyOf(serviceIds);
    }

    public void setGatewayAccountIds(List<String> gatewayAccountIds) {
        this.gatewayAccountIds = List.copyOf(gatewayAccountIds);
    }

    public void setStatus(AgreementStatus status) {
        this.status = status;
    }

    @QueryParam("page")
    public void setPageNumber(Long pageNumber) {
        this.pageNumber = Objects.requireNonNullElse(pageNumber, DEFAULT_PAGE_NUMBER);
    }

    @Parameter(example = "10", schema = @Schema(defaultValue = "20"))
    @DefaultValue("20")
    @QueryParam("display_size")
    public void setDisplaySize(Long displaySize) {
        this.displaySize = displaySize;
    }

    public void setExactReferenceMatch(Boolean exactReferenceMatch) {
        this.exactReferenceMatch = exactReferenceMatch;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public List<String> getFilterTemplates() {
        List<String> filters = new ArrayList<>();

        if (serviceIds != null && !serviceIds.isEmpty()) {
            filters.add(" a.service_id IN (<" + SERVICE_ID_FIELD + ">)");
        }

        if (gatewayAccountIds != null && !gatewayAccountIds.isEmpty()) {
            filters.add(" a.gateway_account_id IN (<" + GATEWAY_ACCOUNT_ID_FIELD + ">)");
        }

        if (status != null) {
            filters.add(" a.status = :" + STATUS_FIELD);
        }

        if (isNotBlank(reference)) {
            if (exactReferenceMatch != null && exactReferenceMatch) {
                filters.add(" lower(a.reference) = lower(:" + REFERENCE_FIELD + ")");
            } else {
                filters.add(" lower(a.reference) LIKE lower(:" + REFERENCE_FIELD + ")");
            }
        }

        if (live != null) {
            filters.add(" a.live = :" + LIVE_FIELD);
        }
        return List.copyOf(filters);
    }

    public Map<String, Object> getQueryMap() {
        if (queryMap == null) {
            queryMap = new HashMap<>();

            if (serviceIds != null && !serviceIds.isEmpty()) {
                queryMap.put(SERVICE_ID_FIELD, serviceIds);
            }

            if (gatewayAccountIds != null && !gatewayAccountIds.isEmpty()) {
                queryMap.put(GATEWAY_ACCOUNT_ID_FIELD, gatewayAccountIds);
            }

            if (status != null) {
                queryMap.put(STATUS_FIELD, status);
            }

            if (isNotBlank(reference)) {
                if (exactReferenceMatch != null && exactReferenceMatch) {
                    queryMap.put(REFERENCE_FIELD, reference);
                } else {
                    queryMap.put(REFERENCE_FIELD, likeClause(reference));
                }
            }

            if (live != null) {
                queryMap.put(LIVE_FIELD, live);
            }
        }
        return Map.copyOf(queryMap);
    }

    public List<String> getGatewayAccountIds() {
        return gatewayAccountIds;
    }

    public List<String> getServiceIds() {
        return serviceIds;
    }

    public AgreementStatus getStatus() {
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
            queries.add(SERVICE_ID_FIELD + "=" + String.join(",", serviceIds));
        }

        if (gatewayAccountIds != null && !gatewayAccountIds.isEmpty()) {
            queries.add(GATEWAY_ACCOUNT_ID_FIELD + "=" + String.join(",", gatewayAccountIds));
        }

        if (status != null) {
            queries.add(STATUS_FIELD + "=" + status);
        }

        if (isNotBlank(reference)) {
            queries.add(REFERENCE_FIELD + "=" + reference);
        }

        if (live != null) {
            queries.add(LIVE_FIELD + "=" + live);
        }

        queries.add("page=" + forPage);
        queries.add("display_size=" + getDisplaySize());

        return String.join("&", queries);
    }

    private String likeClause(String rawUserInputText) {
        return "%" + rawUserInputText + "%";
    }
}