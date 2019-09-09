package uk.gov.pay.ledger.queue.managed;

import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.app.LedgerConfig;
import uk.gov.pay.ledger.app.config.QueueMessageReceiverConfig;
import uk.gov.pay.ledger.queue.EventMessageHandler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class QueueMessageReceiver implements Managed {

    private static final String QUEUE_MESSAGE_RECEIVER_THREAD_NAME = "queue-message-receiver-%d";
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueMessageReceiver.class);
    private final QueueMessageReceiverConfig config;

    private final int queueReadScheduleNumberOfThreads;

    private ScheduledExecutorService scheduledExecutorService;
    private EventMessageHandler eventMessageHandler;

    @Inject
    public QueueMessageReceiver(
            Environment environment,
            LedgerConfig configuration,
            EventMessageHandler eventMessageHandler) {
        this.eventMessageHandler = eventMessageHandler;
        this.config = configuration.getQueueMessageReceiverConfig();
        this.queueReadScheduleNumberOfThreads = config.getNumberOfThreads();

        scheduledExecutorService = environment
                .lifecycle()
                .scheduledExecutorService(QUEUE_MESSAGE_RECEIVER_THREAD_NAME)
                .threads(queueReadScheduleNumberOfThreads)
                .build();
    }

    @Override
    public void start() {
        long initialDelay = config.getThreadDelayInMilliseconds();
        long delay = config.getThreadDelayInMilliseconds();

        for(int i = 0; i < queueReadScheduleNumberOfThreads; i++) {
            scheduledExecutorService.scheduleWithFixedDelay(
                    this::receive,
                    initialDelay,
                    delay,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    private void receive() {
        LOGGER.info("Queue message receiver thread polling queue");
        try {
            eventMessageHandler.handle();
        } catch (Exception e) {
            LOGGER.error("Queue message receiver thread exception", e);
        }
    }

    @Override
    public void stop() {
        scheduledExecutorService.shutdown();
    }
}
