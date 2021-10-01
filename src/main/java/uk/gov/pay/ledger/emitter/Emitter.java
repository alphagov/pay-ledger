package uk.gov.pay.ledger.emitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.net.URI;

public class Emitter {
    private static final Logger LOGGER = LoggerFactory.getLogger(Emitter.class);

    public static void pubTopic(String message) {
        LOGGER.warn(String.format("Publishing message %s", message));

        AwsCredentialsProvider awsCredentialsProvider =
                StaticCredentialsProvider.create(AwsBasicCredentials.create("accesskey", "secretaccesskey"));

        SnsClient snsClient = SnsClient
                .builder()
                .region(Region.EU_WEST_1)
                .credentialsProvider(awsCredentialsProvider)
                .endpointOverride(URI.create("http://localstack:4566"))
                .build();
        try {
            PublishRequest request = PublishRequest.builder()
                    .message(message)
                    .topicArn("arn:aws:sns:eu-west-1:000000000000:payment_events")
                    .build();

            PublishResponse result = snsClient.publish(request);
            System.out.println(result.messageId() + " Message sent. Status is " + result.sdkHttpResponse().statusCode());

        } catch (SnsException e) {
            throw new RuntimeException("FAILED!!!!!!!!!!!!!!!!!!!!! to publish message " + e.getMessage());
        }
    }
}
