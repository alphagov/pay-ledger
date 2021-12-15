package uk.gov.pay.ledger.eventpublisher;

import com.google.inject.Inject;
import uk.gov.pay.ledger.app.LedgerConfig;

import java.util.Map;

class TopicNameArnMapper {
    private final Map<TopicName, String> nameToArnMap;

    @Inject
    TopicNameArnMapper(LedgerConfig ledgerConfig) {
        this.nameToArnMap  = Map.of(
                TopicName.CARD_PAYMENT_EVENTS,
                ledgerConfig.getSnsConfig().getCardPaymentEventsTopicArn()
        );
    }
    public String getArnForTopicName(TopicName topicName) {
        return nameToArnMap.get(topicName);
    }
}
