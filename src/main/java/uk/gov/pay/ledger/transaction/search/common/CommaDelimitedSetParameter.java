package uk.gov.pay.ledger.transaction.search.common;

import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class CommaDelimitedSetParameter {
    private Set<String> elements;
    private String queryString;

    public CommaDelimitedSetParameter(String queryString) {
        this.queryString = queryString;
        elements = isBlank(queryString)
                ? new HashSet<>()
                : Sets.newHashSet(queryString.split(","));
    }

    public Stream<String> stream() {
        return elements.stream();
    }

    public boolean has(String element) {
        return elements.contains(element);
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public String getRawString() {
        return queryString;
    }
}
