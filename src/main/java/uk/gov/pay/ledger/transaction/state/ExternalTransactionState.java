package uk.gov.pay.ledger.transaction.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalTransactionState {

    private final String value;
    private final boolean finished;
    private final String code;
    private final String message;

    public static ExternalTransactionState from(TransactionState state, int statusVersion) {
        String status = statusVersion == 2 ? state.getStatus() : state.getOldStatus();
        return new ExternalTransactionState(status, state.isFinished(),
                state.getCode(), state.getMessage());
    }

    private ExternalTransactionState(String value, boolean finished, String code, String message) {
        this.value = value;
        this.finished = finished;
        this.code = code;
        this.message = message;
    }

    @Schema(example = "cancelled")
    public String getStatus() {
        return value;
    }

    @Schema(example = "true")
    public boolean isFinished() {
        return finished;
    }

    @Schema(example = "P0030")
    public String getCode() {
        return code;
    }

    @Schema(example = "Payment was cancelled by the user")
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ExternalTransactionState{" +
                "value='" + value + '\'' +
                ", finished=" + finished +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
