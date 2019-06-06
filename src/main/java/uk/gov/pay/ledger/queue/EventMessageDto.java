package uk.gov.pay.ledger.queue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventMessageDto {

    @JsonProperty("id")
    private String id;

    public String getId() {
        return id;
    }
}
