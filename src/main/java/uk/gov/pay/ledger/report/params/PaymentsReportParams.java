package uk.gov.pay.ledger.report.params;

import uk.gov.pay.commons.validation.ValidDate;

import javax.ws.rs.QueryParam;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class PaymentsReportParams {
    private static final String GATEWAY_ACCOUNT_EXTERNAL_FIELD = "account_id";
    private static final String FROM_DATE_FIELD = "from_date";
    private static final String TO_DATE_FIELD = "to_date";

    private String accountId;

    @QueryParam("from_date")
    @ValidDate(message = "Invalid attribute value: from_date. Must be a valid date")
    private String fromDate;

    @QueryParam("to_date")
    @ValidDate(message = "Invalid attribute value: to_date. Must be a valid date")
    private String toDate;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public List<String> getFilterTemplates() {
        List<String> filters = new ArrayList<>();

        if (isNotBlank(accountId)) {
            filters.add(" t.gateway_account_id = :" + GATEWAY_ACCOUNT_EXTERNAL_FIELD);
        }
        if (isNotBlank(fromDate)) {
            filters.add(" t.created_date > :" + FROM_DATE_FIELD);
        }
        if (isNotBlank(toDate)) {
            filters.add(" t.created_date < :" + TO_DATE_FIELD);
        }

        return filters;
    }

    public Map<String, Object> getQueryMap() {
        Map<String, Object> queryMap = new HashMap<>();

        if (isNotBlank(accountId)) {
            queryMap.put(GATEWAY_ACCOUNT_EXTERNAL_FIELD, accountId);
        }
        if (isNotBlank(fromDate)) {
            queryMap.put(FROM_DATE_FIELD, ZonedDateTime.parse(fromDate));
        }
        if (isNotBlank(toDate)) {
            queryMap.put(TO_DATE_FIELD, ZonedDateTime.parse(toDate));
        }

        return queryMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentsReportParams that = (PaymentsReportParams) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(fromDate, that.fromDate) &&
                Objects.equals(toDate, that.toDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, fromDate, toDate);
    }

    @Override
    public String toString() {
        return "PaymentsReportParams{" +
                "accountId='" + accountId + '\'' +
                ", fromDate='" + fromDate + '\'' +
                ", toDate='" + toDate + '\'' +
                '}';
    }
}
