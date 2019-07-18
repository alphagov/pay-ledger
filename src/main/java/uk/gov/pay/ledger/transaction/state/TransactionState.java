package uk.gov.pay.ledger.transaction.state;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.ledger.event.model.EventType;

import java.util.Map;

import static java.util.Arrays.stream;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TransactionState {

    CREATED("created", false),
    STARTED("started", false),
    SUBMITTED("submitted", false),
    SUCCESS("success", true),
    FAILED_REJECTED("declined", true),
    FAILED_EXPIRED("timedout", true),
    FAILED_CANCELLED("cancelled", true),
    CANCELLED("cancelled", true),
    ERROR_GATEWAY("error", true);

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

    private static final Map<EventType, TransactionState> EVENT_TYPE_TRANSACTION_STATE_MAP =
            Map.ofEntries(
                    Map.entry(EventType.PAYMENT_CREATED, CREATED),
                    Map.entry(EventType.PAYMENT_STARTED, STARTED),
                    Map.entry(EventType.PAYMENT_EXPIRED, FAILED_EXPIRED),
                    Map.entry(EventType.AUTHORISATION_SUCCESSFUL, SUBMITTED),
                    Map.entry(EventType.AUTHORISATION_REJECTED, FAILED_REJECTED),
                    Map.entry(EventType.AUTHORISATION_SUCCEEDED, SUCCESS),
                    Map.entry(EventType.AUTHORISATION_CANCELLED, FAILED_CANCELLED),
                    Map.entry(EventType.GATEWAY_ERROR_DURING_AUTHORISATION, ERROR_GATEWAY),
                    Map.entry(EventType.GATEWAY_TIMEOUT_DURING_AUTHORISATION, ERROR_GATEWAY),
                    Map.entry(EventType.UNEXPECTED_GATEWAY_ERROR_DURING_AUTHORISATION, ERROR_GATEWAY),
                    Map.entry(EventType.GATEWAY_REQUIRES_3DS_AUTHORISATION, STARTED),
                    Map.entry(EventType.CAPTURE_CONFIRMED, SUCCESS),
                    Map.entry(EventType.CAPTURE_SUBMITTED, SUCCESS),
                    Map.entry(EventType.CAPTURE_ERRORED, ERROR_GATEWAY),
                    Map.entry(EventType.CAPTURE_ABANDONED_AFTER_TOO_MANY_RETRIES, ERROR_GATEWAY),
                    Map.entry(EventType.USER_APPROVED_FOR_CAPTURE_AWAITING_SERVICE_APPROVAL, SUBMITTED),
                    Map.entry(EventType.SERVICE_APPROVED_FOR_CAPTURE, SUCCESS),
                    Map.entry(EventType.CANCEL_BY_EXPIRATION_SUBMITTED, FAILED_EXPIRED),
                    Map.entry(EventType.CANCEL_BY_EXPIRATION_FAILED, FAILED_EXPIRED),
                    Map.entry(EventType.CANCELLED_BY_EXPIRATION, FAILED_EXPIRED),
                    Map.entry(EventType.CANCEL_BY_EXTERNAL_SERVICE_SUBMITTED, CANCELLED),
                    Map.entry(EventType.CANCELLED_BY_EXTERNAL_SERVICE, CANCELLED),
                    Map.entry(EventType.CANCEL_BY_USER_SUBMITTED, FAILED_CANCELLED),
                    Map.entry(EventType.CANCEL_BY_USER_FAILED, FAILED_CANCELLED),
                    Map.entry(EventType.CANCELLED_BY_USER, FAILED_CANCELLED)
            );

    public static TransactionState fromSalientEventType(EventType eventType) {
        return EVENT_TYPE_TRANSACTION_STATE_MAP.get(eventType);
    }

    public static TransactionState from(String transactionState) {
        return stream(values()).filter(v -> v.getState().equals(transactionState)).findFirst()
                .orElse(null);
    }
}
