package uk.gov.pay.ledger.payout.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.payout.state.PayoutState;

public class PayoutEntityFactory {

    private final ObjectMapper objectMapper;

    public PayoutEntityFactory(){
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public PayoutEntity create(EventDigest eventDigest) {
        PayoutState digestPayoutState = eventDigest
                .getMostRecentSalientEventType()
                .map(PayoutState::fromEventType)
                .orElse(PayoutState.UNDEFINED);

        PayoutEntity entity = objectMapper.convertValue(eventDigest.getEventPayload(), PayoutEntity.class);
        entity.setStatus(digestPayoutState);
        entity.setCreatedDate(eventDigest.getEventCreatedDate());
        entity.setGatewayPayoutId(eventDigest.getResourceExternalId());
        return entity;
    }

}
