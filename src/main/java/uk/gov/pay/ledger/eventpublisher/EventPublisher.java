package uk.gov.pay.ledger.eventpublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import javax.inject.Inject;

public class EventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventPublisher.class);

    private final SnsClient snsClient;
    private final TopicNameArnMapper topicNameArnMapper;

    @Inject
    public EventPublisher(SnsClient snsClient, TopicNameArnMapper topicNameArnMapper) {
        this.snsClient = snsClient;
        this.topicNameArnMapper = topicNameArnMapper;
    }

    public void publishMessageToTopic(String message, TopicName topicName) throws EventPublisherException {
        var topicArn = topicNameArnMapper.getArnForTopicName(topicName);
        try {
            PublishRequest request = PublishRequest.builder()
                    .message(message)
                    .topicArn(topicArn)
                    .build();
            snsClient.publish(request);
        } catch (Exception e) {
            throw new EventPublisherException("Failed to publish message to SNS", e);
        }
    }
}
