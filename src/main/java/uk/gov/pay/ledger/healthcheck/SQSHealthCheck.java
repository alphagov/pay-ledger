package uk.gov.pay.ledger.healthcheck;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.app.LedgerConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class SQSHealthCheck extends HealthCheck {

    private final AmazonSQS sqsClient;
    private final Logger logger = LoggerFactory.getLogger(SQSHealthCheck.class);
    private List<SqsHealthCheckQueueHolder> checkList = new ArrayList<>();

    @Inject
    public SQSHealthCheck(AmazonSQS sqsClient, LedgerConfig ledgerConfig) {
        this.sqsClient = sqsClient;
        setUpCheckList(ledgerConfig);
    }

    @Override
    protected Result check() throws Exception {
        for (SqsHealthCheckQueueHolder checkValues : checkList) {
            Optional<String> result = checkQueue(checkValues.queueUrl);
            if (result.isPresent()) {
                return Result.unhealthy(format("failed to retrieve %s queue attributes: %s", checkValues.queueIdentifier, result.get()));
            }
        }

        return Result.healthy();
    }

    private void setUpCheckList(LedgerConfig ledgerConfig) {
        checkList.add(new SqsHealthCheckQueueHolder(ledgerConfig.getSqsConfig().getEventQueueUrl(), "event"));
    }

    private Optional<String> checkQueue(String queueUrl) {
        GetQueueAttributesRequest queueAttributesRequest = new GetQueueAttributesRequest(queueUrl).withAttributeNames("All");
        try {
            sqsClient.getQueueAttributes(queueAttributesRequest);
        } catch (AmazonSQSException | UnsupportedOperationException e) {
            logger.error("Failed to retrieve queue attributes - {}", e.getMessage());
            return Optional.of(e.getMessage());
        } catch (SdkClientException e) {
            logger.error("Failed to connect to sqs queue - {}", e.getMessage());
        }

        return Optional.empty();
    }

    private class SqsHealthCheckQueueHolder {
        private String queueUrl;
        private String queueIdentifier;

        public SqsHealthCheckQueueHolder(String queueUrl, String queueIdentifier) {
            this.queueUrl = queueUrl;
            this.queueIdentifier = queueIdentifier;
        }
    }
}
