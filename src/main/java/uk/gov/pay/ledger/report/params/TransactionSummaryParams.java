package uk.gov.pay.ledger.report.params;

import uk.gov.pay.commons.validation.ValidDate;

import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TransactionSummaryParams implements ReportParams {

    @QueryParam("account_id")
    private String accountId;

    @QueryParam("from_date")
    @ValidDate(message = "Invalid attribute value: from_date. Must be a valid date")
    @NotNull(message = "Field [from_date] can not be null")
    private String fromDate;

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public List<String> getFilterTemplates() {
        List<String> filters = new ArrayList<>();

        if (isNotBlank(accountId)) {
            filters.add(" t.gateway_account_id = :" + GATEWAY_ACCOUNT_EXTERNAL_FIELD);
        }
        filters.add(" t.created_date > :" + FROM_DATE_FIELD);

        return filters;
    }

    public Map<String, Object> getQueryMap() {
        Map<String, Object> queryMap = new HashMap<>();

        if (isNotBlank(accountId)) {
            queryMap.put(GATEWAY_ACCOUNT_EXTERNAL_FIELD, accountId);
        }
        queryMap.put(FROM_DATE_FIELD, ZonedDateTime.parse(fromDate));

        return queryMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionSummaryParams that = (TransactionSummaryParams) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(fromDate, that.fromDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, fromDate);
    }

    @Override
    public String toString() {
        return "TransactionSummaryParams{" +
                "accountId='" + accountId + '\'' +
                ", fromDate='" + fromDate + '\'' +
                '}';
    }
}
