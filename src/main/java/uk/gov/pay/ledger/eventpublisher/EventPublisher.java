package uk.gov.pay.ledger.eventpublisher;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import javax.inject.Inject;

public class EventPublisher {
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
            var errorMessage = String.format("Failed to publish message to SNS: %s", e.getMessage());
            throw new EventPublisherException(errorMessage, e);
        }
    }
}
