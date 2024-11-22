package uk.gov.pay.ledger.transaction.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public class Exemption {

    @Schema(
        example = "true",
        description = "It is set when the exemption has been requested or when we know that it has not been requested."
    )
    private boolean requested;

    public Exemption(boolean requested) {
        this.requested = requested;
    }

    @JsonProperty("requested")
    public boolean getRequested() {
        return requested;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exemption)) return false;

        Exemption exemption = (Exemption) o;

        return requested == exemption.requested;
    }

    @Override
    public int hashCode() {
        return Objects.hash(requested);
    }
}
