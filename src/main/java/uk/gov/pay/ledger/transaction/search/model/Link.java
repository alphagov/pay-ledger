package uk.gov.pay.ledger.transaction.search.model;


import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Link {

    private String href;
    private String method;
    private String rel;
    private String type;
    private Map<String, String> params;

    public Link(String href, String method, String rel, String type, Map<String, String> params) {
        this.href = href;
        this.method = method;
        this.rel = rel;
        this.type = type;
        this.params = params;
    }

    public String getHref() {
        return href;
    }

    public String getMethod() {
        return method;
    }

    public String getRel() { return rel; }

    public String getType() {
        return type;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public static Link ofValue(String href, String method, String rel) {
        return new Link(href, method, rel, null, null);
    }

    public static Link ofValue(String href, String method, String rel, String type, Map<String, String> params) {
        return new Link(href, method, rel, type, params);
    }

    @Override
    public String toString() {
        return "Link{" +
                "rel='" + rel + '\'' +
                ", href='" + href + '\'' +
                ", method='" + method + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link = (Link) o;
        return Objects.equals(href, link.href) &&
                Objects.equals(rel, link.rel) &&
                Objects.equals(method, link.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(href, method);
    }
}
