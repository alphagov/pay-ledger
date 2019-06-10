package uk.gov.pay.ledger.transaction.state;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TransactionState {

    CREATED("created", false);

    private final String value;
    private final boolean finished;
    private final String code;
    private final String message;

    TransactionState(String value, boolean finished) {
        this.value = value;
        this.finished = finished;
        this.code = null;
        this.message = null;
    }

    @JsonProperty("status")
    public String getState() {
        return value;
    }

    public boolean isFinished() {
        return finished;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
