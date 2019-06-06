package uk.gov.pay.ledger.queue;

import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.QueueMessageReceiverConfig;
import uk.gov.pay.ledger.app.config.SqsConfig;
import uk.gov.pay.ledger.queue.sqs.SqsQueueService;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventQueueTest {

    @Mock
    private LedgerConfig ledgerConfig;

    @Mock
    private SqsQueueService sqsQueueService;

    private EventQueue eventQueue;

    @Before
    public void setUp() throws QueueException {
        String validJsonMessage = "{ \"id\": \"my-id\"}";
        SendMessageResult messageResult = mock(SendMessageResult.class);

        List<QueueMessage> messages = Arrays.asList(
                QueueMessage.of(messageResult, validJsonMessage)
        );
        QueueMessageReceiverConfig queueMessageReceiverConfig = mock(QueueMessageReceiverConfig.class);
        SqsConfig sqsConfig = mock(SqsConfig.class);
        when(sqsConfig.getEventQueueUrl()).thenReturn("");
        when(queueMessageReceiverConfig.getMessageRetryDelayInSeconds()).thenReturn(900);
        when(ledgerConfig.getSqsConfig()).thenReturn(sqsConfig);
        when(ledgerConfig.getQueueMessageReceiverConfig()).thenReturn(queueMessageReceiverConfig);
        when(sqsQueueService.receiveMessages(anyString(), anyString())).thenReturn(messages);

        eventQueue = new EventQueue(sqsQueueService, ledgerConfig, new ObjectMapper());
    }

    @Test
    public void retrieveEvents() throws QueueException {
        var eventsList = eventQueue.retrieveEvents();

        assertNotNull(eventsList);
        assertEquals("my-id", eventsList.get(0).getId());
    }
}