package uk.gov.pay.ledger.agreement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.ledger.event.model.Event;

import java.util.List;

public class AgreementEventsResponse {

    @JsonProperty("events")
    private final List<Event> events;
    
    public AgreementEventsResponse(List<Event> events) {
        this.events = events;
    }
    
    public List<Event> getEvents() {
        return events;
    }
    
}
