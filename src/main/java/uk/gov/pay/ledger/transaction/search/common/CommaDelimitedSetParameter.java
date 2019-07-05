package uk.gov.pay.ledger.transaction.search.common;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class CommaDelimitedSetParameter {
    private List<String> elements;
    private String queryString;

    public CommaDelimitedSetParameter(String queryString) {
        this.queryString = queryString;
        elements = isBlank(queryString)
                ? new ArrayList<>()
                : List.of(queryString.split(","));
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public String getRawString() {
        return queryString;
    }

    public List<String> getParameters() {
        return elements;
    }
}
