package uk.gov.pay.ledger.agreement.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.service.payments.commons.model.agreement.AgreementStatus;

import java.util.Map;

public class AgreementsFactory {

    private static final Map<SalientEventType, AgreementStatus> EVENT_TYPE_AGREEMENT_STATE_MAP =
            Map.ofEntries(
                    Map.entry(SalientEventType.AGREEMENT_CREATED, AgreementStatus.CREATED),
                    Map.entry(SalientEventType.AGREEMENT_SET_UP, AgreementStatus.ACTIVE),
                    Map.entry(SalientEventType.AGREEMENT_CANCELLED_BY_SERVICE, AgreementStatus.CANCELLED),
                    Map.entry(SalientEventType.AGREEMENT_CANCELLED_BY_USER, AgreementStatus.CANCELLED),
                    Map.entry(SalientEventType.AGREEMENT_INACTIVATED, AgreementStatus.INACTIVE)
            );

    private final ObjectMapper objectMapper;

    @Inject
    public AgreementsFactory(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }

    public AgreementEntity create(EventDigest eventDigest) {
        var entity = objectMapper.convertValue(eventDigest.getEventAggregate(), AgreementEntity.class);

        entity.setExternalId(eventDigest.getResourceExternalId());
        entity.setCreatedDate(eventDigest.getEventCreatedDate());
        entity.setServiceId(eventDigest.getServiceId());
        entity.setLive(eventDigest.isLive());
        entity.setEventCount(eventDigest.getEventCount());
        entity.setStatus(getStatus(eventDigest, eventDigest.getResourceExternalId()));
        return entity;
    }

    public PaymentInstrumentEntity createPaymentInstrument(EventDigest eventDigest) {
        var entity = objectMapper.convertValue(eventDigest.getEventAggregate(), PaymentInstrumentEntity.class);

        entity.setExternalId(eventDigest.getResourceExternalId());
        entity.setCreatedDate(eventDigest.getEventCreatedDate());
        entity.setEventCount(eventDigest.getEventCount());
        return entity;
    }

    private AgreementStatus getStatus(EventDigest eventDigest, String agreementId) {
        return eventDigest.getMostRecentSalientEventType()
                .map(EVENT_TYPE_AGREEMENT_STATE_MAP::get)
                .orElseThrow(() -> new RuntimeException(String.format("No salient event found for event digest for agreement with external ID: %s", agreementId)));
    }
}