package uk.gov.pay.ledger.queue.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.queue.QueueException;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class SqsQueueService {
    private final Logger logger = LoggerFactory.getLogger(SqsQueueService.class);

    private AmazonSQS sqsClient;

    private final int messageMaximumWaitTimeInSeconds;
    private final int messageMaximumBatchSize;

    @Inject
    public SqsQueueService(AmazonSQS sqsClient, LedgerConfig ledgerConfig) {
        this.sqsClient = sqsClient;
        this.messageMaximumBatchSize = ledgerConfig.getSqsConfig().getMessageMaximumBatchSize();
        this.messageMaximumWaitTimeInSeconds = ledgerConfig.getSqsConfig().getMessageMaximumWaitTimeInSeconds();
    }

    public List<Object> receiveMessages(String queueUrl, String messageAttributeName) throws QueueException {
        try {
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
            receiveMessageRequest
                    .withMessageAttributeNames(messageAttributeName)
                    .withWaitTimeSeconds(messageMaximumWaitTimeInSeconds)
                    .withMaxNumberOfMessages(messageMaximumBatchSize);

            ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);

//            return QueueMessage.of(receiveMessageResult);
            return Collections.emptyList();
        } catch (AmazonSQSException | UnsupportedOperationException e) {
            logger.error("Failed to receive messages from SQS queue - {}", e.getMessage());
            throw new QueueException(e.getMessage());
        }
    }
}
