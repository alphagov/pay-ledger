package uk.gov.pay.ledger.agreement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.ledger.event.model.EventDto;

import java.util.List;

public class AgreementEventsResponse {

    @JsonProperty("events")
    private final List<EventDto> events;
    
    public AgreementEventsResponse(List<EventDto> events) {
        this.events = events;
    }
    
    public List<EventDto> getEvents() {
        return events;
    }
    
}
