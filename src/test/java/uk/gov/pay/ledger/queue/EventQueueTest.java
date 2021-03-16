package uk.gov.pay.ledger.queue;

import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.QueueMessageReceiverConfig;
import uk.gov.pay.ledger.app.config.SqsConfig;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.queue.sqs.SqsQueueService;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventQueueTest {

    @Mock
    private LedgerConfig ledgerConfig;

    @Mock
    private SqsQueueService sqsQueueService;

    private EventQueue eventQueue;

    @BeforeEach
    void setUp() throws QueueException {
        String validJsonMessage = "{" +
                "\"id\": \"my-id\"," +
                "\"timestamp\": \"2018-03-12T16:25:01.123456Z\"," +
                "\"resource_external_id\": \"3uwuyr38rry\"," +
                "\"event_type\":\"PAYMENT_CREATED\"," +
                "\"resource_type\": \"payment\"," +
                "\"reproject_domain_object\": \"true\"," +
                "\"event_details\": {" +
                "\"example_event_details_field\": \"and its value\"" +
                "}" +
        "}";
        SendMessageResult messageResult = mock(SendMessageResult.class);

        List<QueueMessage> messages = List.of(
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
    void retrieveEvents() throws QueueException {
        List<EventMessage> eventsList = eventQueue.retrieveEvents();

        assertNotNull(eventsList);
        assertFalse(eventsList.isEmpty());
        assertEquals("3uwuyr38rry", eventsList.get(0).getEvent().getResourceExternalId());
        assertEquals(ZonedDateTime.parse("2018-03-12T16:25:01.123456Z"), eventsList.get(0).getEvent().getEventDate());
        assertEquals("PAYMENT_CREATED", eventsList.get(0).getEvent().getEventType());
        assertEquals(ResourceType.PAYMENT, eventsList.get(0).getEvent().getResourceType());
        assertEquals("{\"example_event_details_field\":\"and its value\"}", eventsList.get(0).getEvent().getEventData());
        assertTrue(eventsList.get(0).getEvent().isReprojectDomainObject());
    }
}