package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TransactionEventResponse {

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
