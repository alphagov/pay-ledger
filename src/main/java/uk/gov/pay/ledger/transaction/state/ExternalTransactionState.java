package uk.gov.pay.ledger.transaction.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalTransactionState {

    private final String value;
    private final boolean finished;
    private final String code;
    private final String message;

    public ExternalTransactionState(String value, boolean finished) {
        this.value = value;
        this.finished = finished;
        this.code = null;
        this.message = null;
    }

    public ExternalTransactionState(String value, boolean finished, String code, String message) {
        this.value = value;
        this.finished = finished;
        this.code = code;
        this.message = message;
    }

    public String getStatus() {
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
