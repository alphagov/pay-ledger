package uk.gov.pay.ledger.queue.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.SqsConfig;
import uk.gov.pay.ledger.queue.QueueException;
import uk.gov.pay.ledger.queue.QueueMessage;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SqsQueueServiceTest {

    @Mock
    private AmazonSQS sqsClient;

    @Mock
    private LedgerConfig ledgerConfig;

    private SqsQueueService sqsQueueService;

    @Before
    public void setUp() {
        SqsConfig sqsConfig = mock(SqsConfig.class);
        when(sqsConfig.getMessageMaximumBatchSize()).thenReturn(10);
        when(sqsConfig.getMessageMaximumWaitTimeInSeconds()).thenReturn(20);

        when(ledgerConfig.getSqsConfig()).thenReturn(sqsConfig);

        sqsQueueService = new SqsQueueService(sqsClient, ledgerConfig);
    }

    @Test
    public void receiveMessagesShouldReceiveAvailableMessagesFromQueue() throws QueueException {
        ReceiveMessageResult receiveMessageResult = new ReceiveMessageResult();
        Message messageOne = new Message()
                .withMessageId("mock-message-id-1")
                .withReceiptHandle("mock-message-receipt-handle-1")
                .withBody("mock-message-body-1");
        Message messageTwo = new Message()
                .withMessageId("mock-message-id-2")
                .withReceiptHandle("mock-message-receipt-handle-2")
                .withBody("mock-message-body-2");
        receiveMessageResult.getMessages().addAll(List.of(messageOne, messageTwo));

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(receiveMessageResult);

        List<QueueMessage> queueMessages = sqsQueueService.receiveMessages("some-queue-url", "some-attribute-name");
        assertThat(queueMessages.size(), is(2));

        assertThat(queueMessages.get(0).getMessageId(), is("mock-message-id-1"));
        assertThat(queueMessages.get(0).getReceiptHandle(), is("mock-message-receipt-handle-1"));
        assertThat(queueMessages.get(1).getReceiptHandle(), is("mock-message-receipt-handle-2"));
    }


    @Test
    public void receiveMessagesShouldReturnEmptyListWhenNoMessagesAvailable() throws QueueException {
        ReceiveMessageResult receiveMessageResult = new ReceiveMessageResult();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(receiveMessageResult);

        List<QueueMessage> queueMessages = sqsQueueService.receiveMessages("some-queue-url", "some-attribute-name");
        assertTrue(queueMessages.isEmpty());
    }

    @Test(expected = QueueException.class)
    public void receiveMessagesShouldThrowQueueExceptionIfMessagesCannotBeReceived() throws QueueException {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenThrow(AmazonSQSException.class);
        sqsQueueService.receiveMessages("some-queue-url", "some-attribute-name");
    }
}