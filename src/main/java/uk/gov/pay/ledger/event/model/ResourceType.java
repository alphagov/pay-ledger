package uk.gov.pay.ledger.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ResourceType {
    @JsonProperty(value = "agreement")
    AGREEMENT,
    @JsonProperty(value = "charge")
    CHARGE,
    @JsonProperty(value = "refund")
    REFUND,
    @JsonProperty(value = "service")
    SERVICE;
}
