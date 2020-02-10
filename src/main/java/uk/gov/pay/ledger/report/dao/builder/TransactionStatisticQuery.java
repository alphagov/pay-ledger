package uk.gov.pay.ledger.report.dao.builder;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TransactionStatisticQuery {

    public List<String> filters = new ArrayList<>();
    public Map<String, Object> queryMap = new HashMap<>();

    public static final String GATEWAY_ACCOUNT_ID = "account_id";
    public static final String FROM_DATE = "from_date";
    public static final String TO_DATE = "to_date";
    public static final String MOTO = "moto";

    public TransactionStatisticQuery withAccountId(String gatewayAccountId) {
        this.filters.add(" t.gateway_account_id = :" + GATEWAY_ACCOUNT_ID);
        this.queryMap.put(GATEWAY_ACCOUNT_ID, gatewayAccountId);
        return this;
    }

    public TransactionStatisticQuery withFromDate(String fromDate) {
        this.filters.add(" t.created_date > :" + FROM_DATE);
        this.queryMap.put(FROM_DATE, ZonedDateTime.parse(fromDate));
        return this;
    }

    public TransactionStatisticQuery withToDate(String toDate) {
        this.filters.add(" t.created_date < :" + TO_DATE);
        this.queryMap.put(TO_DATE, ZonedDateTime.parse(toDate));
        return this;
    }

    public TransactionStatisticQuery withMoto(Boolean moto) {
        this.filters.add(" t.moto = :" + MOTO);
        this.queryMap.put(MOTO, moto);
        return this;
    }

    public List<String> getFilterTemplates() {
        return filters;
    }

    public Map<String, Object> getQueryMap() {
        return queryMap;
    }

    @Override
    public String toString() {
        return "TransactionStatisticQuery{" +
                "accountId='" + queryMap.get(GATEWAY_ACCOUNT_ID) + '\'' +
                ", moto='" + queryMap.get(MOTO) + '\'' +
                ", fromDate='" + queryMap.get(FROM_DATE) + '\'' +
                ", toDate='" + queryMap.get(TO_DATE) + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionStatisticQuery that = (TransactionStatisticQuery) o;
        return filters.equals(that.filters) &&
                queryMap.equals(that.queryMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filters, queryMap);
    }
}