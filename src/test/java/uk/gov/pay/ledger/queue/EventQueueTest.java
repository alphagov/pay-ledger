package uk.gov.pay.ledger.queue;

import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.QueueMessageReceiverConfig;
import uk.gov.pay.ledger.app.config.SqsConfig;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.queue.sqs.SqsQueueService;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        String validJsonMessage = "{" +
                "\"id\": \"my-id\"," +
                "\"timestamp\": \"2018-03-12T16:25:01.123456Z\"," +
                "\"resource_external_id\": \"3uwuyr38rry\"," +
                "\"event_type\":\"example type\"," +
                "\"resource_type\": \"charge\"," +
                "\"event_details\": {" +
                "\"example_event_details_field\": \"and its value\"" +
                "}" +
        "}";
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
    public void retrieveEvents() throws QueueException, JsonProcessingException {
        List<EventMessage> eventsList = eventQueue.retrieveEvents();

        assertNotNull(eventsList);
        assertFalse(eventsList.isEmpty());
        assertEquals("3uwuyr38rry", eventsList.get(0).getEvent().getResourceExternalId());
        assertEquals(ZonedDateTime.parse("2018-03-12T16:25:01.123456Z"), eventsList.get(0).getEvent().getEventDate());
        assertEquals("example type", eventsList.get(0).getEvent().getEventType());
        assertEquals(ResourceType.CHARGE, eventsList.get(0).getEvent().getResourceType());
        assertEquals("{\"example_event_details_field\":\"and its value\"}", eventsList.get(0).getEvent().getEventData());
    }
}