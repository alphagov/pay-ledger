package uk.gov.pay.ledger.payout.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.payout.state.PayoutState;

import java.util.Map;

public class PayoutEntityFactory {

    private final ObjectMapper objectMapper;
    private static final Logger LOGGER = LoggerFactory.getLogger(PayoutEntityFactory.class);

    @Inject
    public PayoutEntityFactory(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }

    public PayoutEntity create(EventDigest eventDigest) {
        PayoutState digestPayoutState = eventDigest
                .getMostRecentSalientEventType()
                .map(PayoutState::fromEventType)
                .orElse(PayoutState.UNDEFINED);

        String payoutDetails = convertToPayoutDetails(eventDigest.getEventAggregate());
        PayoutEntity entity = objectMapper.convertValue(eventDigest.getEventAggregate(), PayoutEntity.class);
        entity.setState(digestPayoutState);
        entity.setCreatedDate(eventDigest.getEventCreatedDate());
        entity.setGatewayPayoutId(eventDigest.getResourceExternalId());
        entity.setServiceId(eventDigest.getServiceId());
        entity.setLive(eventDigest.isLive());
        entity.setEventCount(eventDigest.getEventCount());
        entity.setPayoutDetails(payoutDetails);
        return entity;
    }

    private String convertToPayoutDetails(Map<String, Object> payoutPayload) {
        try {
            return objectMapper.writeValueAsString(payoutPayload);
        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to parse incoming event payload: {}", e.getMessage());
        }
        return "{}";
    }
}
