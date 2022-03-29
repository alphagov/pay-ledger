package uk.gov.pay.ledger.agreement.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.payout.state.PayoutState;

import java.util.Map;

public class AgreementEntityFactory {

    private final ObjectMapper objectMapper;
    private static final Logger LOGGER = LoggerFactory.getLogger(AgreementEntityFactory.class);

    @Inject
    public AgreementEntityFactory(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }

    public AgreementEntity create(EventDigest eventDigest) {
        var entity = objectMapper.convertValue(eventDigest.getEventAggregate(), AgreementEntity.class);

        entity.setExternalId(eventDigest.getResourceExternalId());
        entity.setCreatedDate(eventDigest.getEventCreatedDate());
        entity.setServiceId(eventDigest.getServiceId());
        entity.setLive(eventDigest.isLive());
        entity.setEventCount(eventDigest.getEventCount());
        return entity;
    }

    public PaymentInstrumentEntity createPaymentInstrument(EventDigest eventDigest) {
        var entity = objectMapper.convertValue(eventDigest.getEventAggregate(), PaymentInstrumentEntity.class);

        entity.setExternalId(eventDigest.getResourceExternalId());
        entity.setCreatedDate(eventDigest.getEventCreatedDate());
        entity.setEventCount(eventDigest.getEventCount());
        return entity;
    }
}