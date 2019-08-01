package uk.gov.pay.ledger.transaction.state;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.SalientEventType;

import java.util.Map;

import static java.util.Arrays.stream;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TransactionState {

    CREATED("created", false),
    STARTED("started", false),
    SUBMITTED("submitted", false),
    CAPTURABLE("capturable", false),
    SUCCESS("success", true),
    FAILED_REJECTED("failed", "declined", true, "P0010", "Payment method rejected"),
    FAILED_EXPIRED("failed", "timedout", true, "P0020", "Payment expired"),
    FAILED_CANCELLED("failed", "cancelled", true, "P0030", "Payment was cancelled by the user"),
    CANCELLED("cancelled", "cancelled", true, "P0040", "Payment was cancelled by the service"),
    ERROR("error", "error", true, "P0050", "Payment provider returned an error"),
    ERROR_GATEWAY("error", "error", true, "P0050", "Payment provider returned an error");

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionState.class);

    private final String status;
    private final String newStatus;
    private final boolean finished;
    private final String code;
    private final String message;

    TransactionState(String status, boolean finished) {
        this.status = status;
        this.newStatus = status;
        this.finished = finished;
        this.code = null;
        this.message = null;
    }

    TransactionState(String status, String newStatus, boolean finished, String code, String message) {
        this.status = status;
        this.newStatus = newStatus;
        this.finished = finished;
        this.code = code;
        this.message = message;
    }

    public String getStatus() {
        return status;
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
            Map.ofEntries(
                    Map.entry(SalientEventType.PAYMENT_CREATED, CREATED),
                    Map.entry(SalientEventType.PAYMENT_STARTED, STARTED),
                    Map.entry(SalientEventType.PAYMENT_EXPIRED, FAILED_EXPIRED),
                    Map.entry(SalientEventType.AUTHORISATION_SUCCESSFUL, SUBMITTED),
                    Map.entry(SalientEventType.AUTHORISATION_REJECTED, FAILED_REJECTED),
                    Map.entry(SalientEventType.AUTHORISATION_SUCCEEDED, SUBMITTED),
                    Map.entry(SalientEventType.AUTHORISATION_CANCELLED, FAILED_CANCELLED),
                    Map.entry(SalientEventType.GATEWAY_ERROR_DURING_AUTHORISATION, ERROR_GATEWAY),
                    Map.entry(SalientEventType.GATEWAY_TIMEOUT_DURING_AUTHORISATION, ERROR_GATEWAY),
                    Map.entry(SalientEventType.UNEXPECTED_GATEWAY_ERROR_DURING_AUTHORISATION, ERROR_GATEWAY),
                    Map.entry(SalientEventType.GATEWAY_REQUIRES_3DS_AUTHORISATION, STARTED),
                    Map.entry(SalientEventType.CAPTURE_CONFIRMED, SUCCESS),
                    Map.entry(SalientEventType.CAPTURE_SUBMITTED, SUCCESS),
                    Map.entry(SalientEventType.CAPTURE_ERRORED, ERROR_GATEWAY),
                    Map.entry(SalientEventType.CAPTURE_ABANDONED_AFTER_TOO_MANY_RETRIES, ERROR_GATEWAY),
                    Map.entry(SalientEventType.USER_APPROVED_FOR_CAPTURE, SUCCESS),
                    Map.entry(SalientEventType.USER_APPROVED_FOR_CAPTURE_AWAITING_SERVICE_APPROVAL, SUBMITTED),
                    Map.entry(SalientEventType.SERVICE_APPROVED_FOR_CAPTURE, SUCCESS),
                    Map.entry(SalientEventType.CANCEL_BY_EXPIRATION_SUBMITTED, FAILED_EXPIRED),
                    Map.entry(SalientEventType.CANCEL_BY_EXPIRATION_FAILED, FAILED_EXPIRED),
                    Map.entry(SalientEventType.CANCELLED_BY_EXPIRATION, FAILED_EXPIRED),
                    Map.entry(SalientEventType.CANCEL_BY_EXTERNAL_SERVICE_SUBMITTED, CANCELLED),
                    Map.entry(SalientEventType.CANCELLED_BY_EXTERNAL_SERVICE, CANCELLED),
                    Map.entry(SalientEventType.CANCEL_BY_USER_SUBMITTED, FAILED_CANCELLED),
                    Map.entry(SalientEventType.CANCEL_BY_USER_FAILED, FAILED_CANCELLED),
                    Map.entry(SalientEventType.CANCELLED_BY_USER, FAILED_CANCELLED),
                    Map.entry(SalientEventType.REFUND_CREATED_BY_SERVICE, SUBMITTED),
                    Map.entry(SalientEventType.REFUND_CREATED_BY_USER, SUBMITTED),
                    Map.entry(SalientEventType.REFUND_SUBMITTED, SUBMITTED),
                    Map.entry(SalientEventType.REFUND_SUCCEEDED, SUCCESS),
                    Map.entry(SalientEventType.REFUND_ERROR, ERROR)
            );

    public static TransactionState fromEventType(SalientEventType salientEventType) {
        return EVENT_TYPE_TRANSACTION_STATE_MAP.get(salientEventType);
    }

    public static TransactionState from(String transactionState) {
        return stream(values()).filter(v -> v.getStatus().equals(transactionState)).findFirst()
                .orElseGet(() -> {
                    LOGGER.warn("Unknown transaction state {}", transactionState);
                    return null;
                });
    }
}
