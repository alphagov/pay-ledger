package uk.gov.pay.ledger.payout.state;

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
public enum PayoutState {
    UNDEFINED("undefined", false),
    IN_TRANSIT("intransit", false),
    PAID_OUT("paidout", true),
    FAILED("failed", true);

    private final String status;
    private final boolean finished;

    private static final Logger LOGGER = LoggerFactory.getLogger(PayoutState.class);

    PayoutState(String status, boolean finished) {
        this.status = status;
        this.finished = finished;
    }

    public String getStatus() {
        return status;
    }

    @JsonProperty("finished")
    public boolean isFinished() {
        return finished;
    }

    private static final Map<SalientEventType, PayoutState> EVENT_TYPE_PAYOUT_STATE_MAP = Map.ofEntries(
            Map.entry(SalientEventType.PAYOUT_CREATED, IN_TRANSIT),
            Map.entry(SalientEventType.PAYOUT_PAID_OUT, PAID_OUT),
            Map.entry(SalientEventType.PAYOUT_BANK_DECLINED, FAILED),
            Map.entry(SalientEventType.PAYOUT_INSUFFICIENT_FUNDS, FAILED),
            Map.entry(SalientEventType.PAYOUT_FAILED_UNKNOWN, FAILED)
    );
    public static PayoutState fromEventType(SalientEventType salientEventType) {
        return EVENT_TYPE_PAYOUT_STATE_MAP.get(salientEventType);
    }
    public static PayoutState from(String payoutState) {
        return stream(values()).filter(v -> v.name().equals(payoutState)).findFirst()
                .orElseGet(() -> {
                    LOGGER.warn("Unknown payout state {}", payoutState);
                    return null;
                });
    }
}
