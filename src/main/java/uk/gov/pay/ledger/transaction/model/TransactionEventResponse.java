package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TransactionEventResponse {

    @Schema(example = "9np5pocnotgkpp029d5kdfau5f")
    private final String transactionId;
    private final List<TransactionEvent> events;

    private TransactionEventResponse(String externalId, List<TransactionEvent> events) {
        this.transactionId = externalId;
        this.events = events;
    }

    public static TransactionEventResponse of(String externalId, List<TransactionEvent> events) {
        return new TransactionEventResponse(externalId, events);
    }

    public String getTransactionId() {
        return transactionId;
    }

    public List<TransactionEvent> getEvents() {
        return events;
    }
}
