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

public class TransactionSummaryParams {
    private static final String TO_DATE_FIELD = "to_date";
    private static final String GATEWAY_ACCOUNT_EXTERNAL_FIELD = "account_id";
    private static final String MOTO = "moto";
    private static final String FROM_DATE_FIELD = "from_date";

    @QueryParam("account_id")
    private String accountId;

    @QueryParam("moto")
    private boolean moto;

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

    public boolean isMoto() {
        return moto;
    }

    public void setMoto(boolean moto) {
        this.moto = moto;
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

    public List<String> getFilterTemplatesWithMoto() {
        List<String> filters = getFilterTemplates();

        if (isMoto()) {
            filters.add(" t.moto = :" + MOTO);
        }

        return filters;
    }

    public Map<String, Object> getQueryMap() {
        Map<String, Object> queryMap = new HashMap<>();

        if (isNotBlank(accountId)) {
            queryMap.put(GATEWAY_ACCOUNT_EXTERNAL_FIELD, accountId);
        }

        if (moto) {
            queryMap.put(MOTO, true);
        }

        if (isNotBlank(fromDate)) {
            queryMap.put(FROM_DATE_FIELD, ZonedDateTime.parse(fromDate));
        }
        if (isNotBlank(toDate)) {
            queryMap.put(TO_DATE_FIELD, ZonedDateTime.parse(toDate));
        }

        return queryMap;
    }

    public Map<String, Object> getQueryMapWithMoto() {
        Map<String, Object> queryMap = getQueryMap();
        queryMap.put(MOTO, true);

        return queryMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionSummaryParams that = (TransactionSummaryParams) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(moto, that.moto) &&
                Objects.equals(fromDate, that.fromDate) &&
                Objects.equals(toDate, that.toDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, moto, fromDate, toDate);
    }

    @Override
    public String toString() {
        return "TransactionSummaryParams{" +
                "accountId='" + accountId + '\'' +
                "moto='" + moto + '\'' +
                ", fromDate='" + fromDate + '\'' +
                ", toDate='" + toDate + '\'' +
                '}';
    }
}
