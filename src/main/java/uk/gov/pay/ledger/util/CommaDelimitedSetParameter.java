package uk.gov.pay.ledger.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class CommaDelimitedSetParameter {
    private Set<String> elements;
    private String queryString;

    public CommaDelimitedSetParameter(String queryString) {
        this.queryString = queryString;
        elements = isBlank(queryString)
                ? new HashSet<>()
                : Set.of(queryString.split(","));
    }

    public boolean isNotEmpty() {
        return !elements.isEmpty();
    }

    public String getRawString() {
        return queryString;
    }

    public Set<String> getParameters() {
        return elements;
    }
}
