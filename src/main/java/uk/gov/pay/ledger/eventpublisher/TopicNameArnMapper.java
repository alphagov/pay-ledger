package uk.gov.pay.ledger.eventpublisher;

import com.google.inject.Inject;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.SnsConfig;

import java.util.Map;

class TopicNameArnMapper {
    private final SnsConfig snsConfig;

    @Inject
    TopicNameArnMapper(LedgerConfig ledgerConfig) {
        this.snsConfig  = ledgerConfig.getSnsConfig();
    }
    public String getArnForTopicName(TopicName topicName) {
        return Map.of(TopicName.CARD_PAYMENT_EVENTS, snsConfig.getCardPaymentEventsTopicArn())
                .get(topicName);
    }
}
