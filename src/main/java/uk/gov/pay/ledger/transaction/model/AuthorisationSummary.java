package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public class AuthorisationSummary {

    private ThreeDSecure threeDSecure;

    public AuthorisationSummary(ThreeDSecure threeDSecure) {
        this.threeDSecure = threeDSecure;
    }

    @JsonProperty("three_d_secure")
    public ThreeDSecure getThreeDSecure() {
        return threeDSecure;
    }

    @Override
    public String toString() {
        return "AuthorisationSummary{" +
                "threeDSecure=" + threeDSecure +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorisationSummary authorisationSummary = (AuthorisationSummary) o;
        return Objects.equals(threeDSecure, authorisationSummary.threeDSecure);
    }

    @Override
    public int hashCode() {
        return Objects.hash(threeDSecure);
    }
}
