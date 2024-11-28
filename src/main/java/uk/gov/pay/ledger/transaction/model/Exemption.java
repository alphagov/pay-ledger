package uk.gov.pay.ledger.transaction.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public class Exemption {
        @JsonInclude(Include.NON_NULL)
        public static final class Outcome {
            public Outcome(Exemption3ds result) {
                this.result = result;
            }

            @JsonProperty("result")
            @Schema(description = "Indicating the result if 3ds exemption was requested for the payment.", example = "honoured")
            @JsonSerialize(using = ToStringSerializer.class)
            private final Exemption3ds result;

            public Exemption3ds getResult() {
                return result;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Outcome that = (Outcome) o;
                return Objects.equals(result, that.result);
            }

            @Override
            public int hashCode() {
                return Objects.hash(result);
            }

            @Override
            public String toString() {
                return "Outcome{" +
                        "result='" + result + '\'' +
                        '}';
            }
        }

    @JsonProperty("requested")
    @Schema(description = "Flag indicating whether 3ds exemption was requested for the payment.", example = "true")
    private final boolean requested;

    @JsonProperty("type")
    @JsonInclude(Include.NON_NULL)
    @Schema(description = "Indicating the type of the 3ds exemption was requested for the payment if applicable.", example = "corporate")
    private final String type;

    @JsonProperty("outcome")
    @JsonInclude(Include.NON_NULL)
    @Schema(description = "Object containing information about the outcome of the 3ds exemption request", example = "honoured")
    private final Outcome outcome;

    public Exemption(boolean requested, String type, Outcome outcome) {
        this.requested = requested;
        this.type = type;
        this.outcome = outcome;
    }

    @JsonProperty("requested")
    public boolean isRequested() {
        return requested;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public Outcome getOutcome() {
        return outcome;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Exemption that = (Exemption) o;
        return Objects.equals(requested, that.requested)
                && Objects.equals(type, that.type)
                && Objects.equals(outcome, that.outcome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requested, type, outcome);
    }

    @Override
    public String toString() {
        return "Exemption{" +
                "requested='" + requested + '\'' +
                ", type='" + type + '\'' +
                ", outcome=" + (outcome == null ? "''" : outcome.toString()) +
                '}';
    }
}
