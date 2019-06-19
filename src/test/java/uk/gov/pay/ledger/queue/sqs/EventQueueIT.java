package uk.gov.pay.ledger.queue.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.QueueMessageReceiverConfig;
import uk.gov.pay.ledger.app.config.SqsConfig;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.queue.EventMessage;
import uk.gov.pay.ledger.queue.EventQueue;
import uk.gov.pay.ledger.queue.QueueException;
import uk.gov.pay.ledger.queue.QueueMessage;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.rule.SqsTestDocker;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.util.fixture.QueueEventFixture.aQueueEventFixture;

@Ignore
public class EventQueueIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();
    private AmazonSQS client;

    @Before
    public void setUp() {
        client = rule.getSqsClient();
    }

    @Test
    public void shouldGetEventMessageDtoFromTheQueue() throws QueueException {
        Event event = aQueueEventFixture()
                .insert(client)
                .toEntity();

        SqsConfig sqsConfig = mock(SqsConfig.class);
        when(sqsConfig.getMessageMaximumBatchSize()).thenReturn(10);
        when(sqsConfig.getMessageMaximumWaitTimeInSeconds()).thenReturn(1);
        LedgerConfig mockConfig = mock(LedgerConfig.class);
        when(mockConfig.getSqsConfig()).thenReturn(sqsConfig);

        SqsQueueService x = new SqsQueueService(client, mockConfig);

        List<QueueMessage> result = x.receiveMessages(SqsTestDocker.getQueueUrl("event-queue"), "All");
        assertFalse(result.isEmpty());
    }

    @Test
    public void shouldGetEventMessageFromTheQueue() throws QueueException {
        Event event = aQueueEventFixture()
                .insert(client)
                .toEntity();

        SqsConfig sqsConfig = mock(SqsConfig.class);
        when(sqsConfig.getMessageMaximumBatchSize()).thenReturn(10);
        when(sqsConfig.getMessageMaximumWaitTimeInSeconds()).thenReturn(1);
        when(sqsConfig.getEventQueueUrl()).thenReturn(SqsTestDocker.getQueueUrl("event-queue"));
        QueueMessageReceiverConfig queueReceiverConfig = mock(QueueMessageReceiverConfig.class);
        when(queueReceiverConfig.getMessageRetryDelayInSeconds()).thenReturn(10);
        LedgerConfig mockConfig = mock(LedgerConfig.class);
        when(mockConfig.getSqsConfig()).thenReturn(sqsConfig);
        when(mockConfig.getQueueMessageReceiverConfig()).thenReturn(queueReceiverConfig);

        SqsQueueService x = new SqsQueueService(client, mockConfig);
        EventQueue y = new EventQueue(x, mockConfig, new ObjectMapper());

        List<EventMessage> result = y.retrieveEvents();
        assertFalse(result.isEmpty());
        assertThat(result.get(0).getId(), is(event.getResourceExternalId()));
    }
}