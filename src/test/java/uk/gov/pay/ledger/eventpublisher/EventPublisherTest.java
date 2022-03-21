package uk.gov.pay.ledger.eventpublisher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.SnsConfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventPublisherTest {
    @Mock
    private SnsClient snsClient;

    @Mock
    private LedgerConfig ledgerConfig;

    @Mock
    SnsConfig snsConfig;

    private TopicNameArnMapper topicNameArnMapper;

    private String cardPaymentEventsTopicArn = "card_payment_events_arn";

    private String cardPaymentDisputeEventsTopicArn = "card_payment_dispute_events_arn";

    private EventPublisher eventPublisher;

    @Test
    public void shouldPublishMessageToSpecifiedTopic() throws Exception {
        when(ledgerConfig.getSnsConfig()).thenReturn(snsConfig);
        when(snsConfig.getCardPaymentEventsTopicArn()).thenReturn(cardPaymentEventsTopicArn);
        when(snsConfig.getCardPaymentDisputeEventsTopicArn()).thenReturn(cardPaymentDisputeEventsTopicArn);
        topicNameArnMapper = new TopicNameArnMapper(ledgerConfig);
        eventPublisher = new EventPublisher(snsClient, topicNameArnMapper);
        var message = "Hooray, I've been published";
        var expectedPublishRequest = PublishRequest
                .builder()
                .message(message)
                .topicArn(cardPaymentEventsTopicArn)
                .build();

        eventPublisher.publishMessageToTopic(message, TopicName.CARD_PAYMENT_EVENTS);

        verify(snsClient).publish(expectedPublishRequest);
    }

    @Test
    public void shouldNotErrorWhenTopicArnNotSet() {
        when(ledgerConfig.getSnsConfig()).thenReturn(snsConfig);
        lenient().when(snsConfig.getCardPaymentEventsTopicArn()).thenReturn(null);
        lenient().when(snsConfig.getCardPaymentDisputeEventsTopicArn()).thenReturn(null);
        topicNameArnMapper = new TopicNameArnMapper(ledgerConfig);

        assertDoesNotThrow(() -> new EventPublisher(snsClient, topicNameArnMapper));
    }
}
