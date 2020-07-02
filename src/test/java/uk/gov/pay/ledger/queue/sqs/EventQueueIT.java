package uk.gov.pay.ledger.queue.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.QueueMessageReceiverConfig;
import uk.gov.pay.ledger.app.config.SqsConfig;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.queue.EventMessage;
import uk.gov.pay.ledger.queue.EventQueue;
import uk.gov.pay.ledger.queue.QueueException;
import uk.gov.pay.ledger.queue.QueueMessage;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.rule.SqsTestDocker;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.pay.ledger.util.fixture.QueuePaymentEventFixture.aQueuePaymentEventFixture;

@Disabled
public class EventQueueIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();
    private AmazonSQS client;

    @BeforeEach
    public void setUp() {
        client = rule.getSqsClient();
    }

    @Test
    public void shouldGetEventMessageDtoFromTheQueue() throws QueueException {
        aQueuePaymentEventFixture().insert(client);

        SqsConfig sqsConfig = mock(SqsConfig.class);
        when(sqsConfig.getMessageMaximumBatchSize()).thenReturn(10);
        when(sqsConfig.getMessageMaximumWaitTimeInSeconds()).thenReturn(1);
        LedgerConfig mockConfig = mock(LedgerConfig.class);
        when(mockConfig.getSqsConfig()).thenReturn(sqsConfig);

        SqsQueueService sqsQueueService = new SqsQueueService(client, mockConfig);

        List<QueueMessage> result = sqsQueueService.receiveMessages(SqsTestDocker.getQueueUrl("event-queue"), "All");
        assertFalse(result.isEmpty());
    }

    @Test
    public void shouldGetEventMessageFromTheQueue() throws QueueException {
        Event event = aQueuePaymentEventFixture()
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

        SqsQueueService sqsQueueService = new SqsQueueService(client, mockConfig);
        EventQueue eventQueue = new EventQueue(sqsQueueService, mockConfig, new ObjectMapper());

        List<EventMessage> result = eventQueue.retrieveEvents();
        assertFalse(result.isEmpty());
        assertThat(result.get(0).getId(), is(event.getResourceExternalId()));
    }
}