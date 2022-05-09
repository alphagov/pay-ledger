package uk.gov.pay.ledger.agreement.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import uk.gov.pay.ledger.event.model.EventDigest;

public class AgreementEntityFactory {

    private final ObjectMapper objectMapper;

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