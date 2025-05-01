package uk.gov.pay.ledger.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import uk.gov.pay.ledger.app.LedgerConfig;

import java.util.Optional;

import static java.lang.String.format;

public class SQSHealthCheck extends HealthCheck {

    private final SqsClient sqsClient;
    private final Logger logger = LoggerFactory.getLogger(SQSHealthCheck.class);
    private NameValuePair sqsHealthCheckHolder;

    @Inject
    public SQSHealthCheck(SqsClient sqsClient, LedgerConfig ledgerConfig) {
        this.sqsClient = sqsClient;
        setUpSQSHealthCheckHolder(ledgerConfig);
    }

    private void setUpSQSHealthCheckHolder(LedgerConfig ledgerConfig) {
        this.sqsHealthCheckHolder = new BasicNameValuePair("event", ledgerConfig.getSqsConfig().getEventQueueUrl());
    }

    @Override
    protected Result check() {
        Optional<String> result = checkQueue(sqsHealthCheckHolder);

        return result
                .map(s -> Result.unhealthy(format("Failed %s queue attribute check: %s", sqsHealthCheckHolder.getName(), s)))
                .orElseGet(Result::healthy);
    }

    private Optional<String> checkQueue(NameValuePair nameValuePair) {
        GetQueueAttributesRequest queueAttributesRequest =
                GetQueueAttributesRequest.builder()
                        .queueUrl(nameValuePair.getValue())
                        .attributeNamesWithStrings("All")
                        .build();
        try {
            sqsClient.getQueueAttributes(queueAttributesRequest);
        } catch (UnsupportedOperationException | SdkClientException e) {
            logger.error("Failed to retrieve [{}] queue attributes - {}", nameValuePair.getName(), e.getMessage());
            return Optional.of("Failed to retrieve queue attributes - " + e);
        }
        return Optional.empty();
    }
}
