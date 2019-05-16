package uk.gov.pay.ledger.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Event {
    private String id;

    public Event() {
    }

    public Event(String id) {
        this.id = id;
    }

    @JsonProperty
    public String getId() {
        return id;
    }
}
