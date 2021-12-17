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

    private String cardPaymentEventTopicArn = "sns_arn";

    private EventPublisher eventPublisher;

    @Test
    public void shouldPublishMessageToSpecifiedTopic() throws Exception {
        when(ledgerConfig.getSnsConfig()).thenReturn(snsConfig);
        when(snsConfig.getCardPaymentEventsTopicArn()).thenReturn(cardPaymentEventTopicArn);
        topicNameArnMapper = new TopicNameArnMapper(ledgerConfig);
        eventPublisher = new EventPublisher(snsClient, topicNameArnMapper);
        var message = "Hooray, I've been published";
        var expectedPublishRequest = PublishRequest
                .builder()
                .message(message)
                .topicArn(cardPaymentEventTopicArn)
                .build();

        eventPublisher.publishMessageToTopic(message, TopicName.CARD_PAYMENT_EVENTS);

        verify(snsClient).publish(expectedPublishRequest);
    }

    @Test
    public void shouldNotErrorWhenTopicArnNotSet() {
        when(ledgerConfig.getSnsConfig()).thenReturn(snsConfig);
        lenient().when(snsConfig.getCardPaymentEventsTopicArn()).thenReturn(null);
        topicNameArnMapper = new TopicNameArnMapper(ledgerConfig);

        assertDoesNotThrow(() -> new EventPublisher(snsClient, topicNameArnMapper));
    }
}
