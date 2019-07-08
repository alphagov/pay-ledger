package uk.gov.pay.ledger.transaction.state;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.ledger.event.model.SalientEventType;

import java.util.Map;

import static java.util.Arrays.stream;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TransactionState {

    CREATED("created", false),
    SUBMITTED("submitted", false);

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

    @JsonProperty("finished")
    public boolean isFinished() {
        return finished;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    private static final Map<SalientEventType, TransactionState> EVENT_TYPE_TRANSACTION_STATE_MAP = Map.of(
            SalientEventType.PAYMENT_CREATED, CREATED,
            SalientEventType.AUTHORISATION_SUCCESSFUL, SUBMITTED
    );

    public static TransactionState fromSalientEventType(SalientEventType salientEventType) {
        return EVENT_TYPE_TRANSACTION_STATE_MAP.get(salientEventType);
    }

    public static TransactionState from(String transactionState) {
        return stream(values()).filter(v -> v.getState().equals(transactionState)).findFirst()
                .orElseGet(null);
    }
}
