package uk.gov.pay.ledger.transaction.state;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import uk.gov.pay.ledger.event.model.SalientEventType;

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

    private static final Map<SalientEventType, TransactionState> EVENT_TYPE_TRANSACTION_STATE_MAP =
            ImmutableMap.<SalientEventType, TransactionState>builder()
                    .put(SalientEventType.PAYMENT_CREATED, CREATED)
                    .put(SalientEventType.PAYMENT_STARTED, STARTED)
                    .put(SalientEventType.PAYMENT_EXPIRED, FAILED_EXPIRED)
                    .put(SalientEventType.AUTHORISATION_SUCCESSFUL, SUBMITTED)
                    .put(SalientEventType.AUTHORISATION_REJECTED, FAILED_REJECTED)
                    .put(SalientEventType.AUTHORISATION_SUCCEEDED, SUCCESS)
                    .put(SalientEventType.AUTHORISATION_CANCELLED, FAILED_CANCELLED)
                    .put(SalientEventType.GATEWAY_ERROR_DURING_AUTHORISATION, ERROR_GATEWAY)
                    .put(SalientEventType.GATEWAY_TIMEOUT_DURING_AUTHORISATION, ERROR_GATEWAY)
                    .put(SalientEventType.UNEXPECTED_GATEWAY_ERROR_DURING_AUTHORISATION, ERROR_GATEWAY)
                    .put(SalientEventType.GATEWAY_REQUIRES_3DS_AUTHORISATION, STARTED)
                    .put(SalientEventType.CAPTURE_CONFIRMED, SUCCESS)
                    .put(SalientEventType.CAPTURE_SUBMITTED, SUCCESS)
                    .put(SalientEventType.CAPTURE_ERRORED, ERROR_GATEWAY)
                    .put(SalientEventType.CAPTURE_ABANDONED_AFTER_TOO_MANY_RETRIES, ERROR_GATEWAY)
                    .put(SalientEventType.USER_APPROVED_FOR_CAPTURE_AWAITING_SERVICE_APPROVAL, SUBMITTED)
                    .put(SalientEventType.SERVICE_APPROVED_FOR_CAPTURE, SUCCESS)
                    .put(SalientEventType.CANCEL_BY_EXPIRATION_SUBMITTED, FAILED_EXPIRED)
                    .put(SalientEventType.CANCEL_BY_EXPIRATION_FAILED, FAILED_EXPIRED)
                    .put(SalientEventType.CANCELLED_BY_EXPIRATION, FAILED_EXPIRED)
                    .put(SalientEventType.CANCEL_BY_EXTERNAL_SERVICE_SUBMITTED, CANCELLED)
                    .put(SalientEventType.CANCELLED_BY_EXTERNAL_SERVICE, CANCELLED)
                    .put(SalientEventType.CANCEL_BY_USER_SUBMITTED, FAILED_CANCELLED)
                    .put(SalientEventType.CANCEL_BY_USER_FAILED, FAILED_CANCELLED)
                    .put(SalientEventType.CANCELLED_BY_USER, FAILED_CANCELLED)
                    .build();

    public static TransactionState fromSalientEventType(SalientEventType salientEventType) {
        return EVENT_TYPE_TRANSACTION_STATE_MAP.get(salientEventType);
    }

    public static TransactionState from(String transactionState) {
        return stream(values()).filter(v -> v.getState().equals(transactionState)).findFirst()
                .orElseGet(null);
    }
}
