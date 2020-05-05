package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ResourceType {
    @JsonProperty(value = "agreement")
    AGREEMENT,
    @JsonProperty(value = "payment")
    PAYMENT,
    @JsonProperty(value = "refund")
    REFUND,
    @JsonProperty(value = "service")
    SERVICE,
    @JsonProperty(value = "card_payment")
    CARD_PAYMENT,
    @JsonProperty(value = "payout")
    PAYOUT
}