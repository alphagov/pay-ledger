package uk.gov.pay.ledger.util.pagination;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationLink {

    @Schema(example = "https://an.example.link")
    private String href;

    private PaginationLink(String href) {
        this.href = href;
    }

    public PaginationLink() {
    }

    public String getHref() {
        return href;
    }

    public static PaginationLink ofValue(String href) {
        return new PaginationLink(href);
    }

    @Override
    public String toString() {
        return "Link{" +
                "href='" + href + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaginationLink link = (PaginationLink) o;
        return Objects.equals(href, link.href);
    }

    @Override
    public int hashCode() {
        return Objects.hash(href);
    }
}
