package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public class ThreeDSecure {

    private boolean requires3ds;
    private String version;

    public ThreeDSecure(boolean requires3ds, String version) {
        this.requires3ds = requires3ds;
        this.version = version;
    }

    @JsonProperty("required")
    public boolean isRequires3ds() {
        return requires3ds;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreeDSecure that = (ThreeDSecure) o;
        return Objects.equals(requires3ds, that.requires3ds) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requires3ds, version);
    }

    @Override
    public String toString() {
        return "ThreeDSecure{" +
                "requires3ds=" + requires3ds +
                ", version='" + version + '\'' +
                '}';
    }
}
