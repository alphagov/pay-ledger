package uk.gov.pay.ledger.app.config;

import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.Instant;

public class QueueMessageReceiverConfig extends Configuration {

    @Valid
    private boolean backgroundProcessingEnabled;

    @Valid
    @NotNull
    private int threadDelayInMilliseconds;

    @Valid
    @NotNull
    private int numberOfThreads;

    @Valid
    @NotNull
    private int messageRetryDelayInSeconds;

    @Valid
    private Instant projectLivePaymentsDisputeEventsFromDate;

    @Valid
    private Instant projectTestPaymentsDisputeEventsFromDate;

    public int getThreadDelayInMilliseconds() {
        return threadDelayInMilliseconds;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public int getMessageRetryDelayInSeconds() {
        return messageRetryDelayInSeconds;
    }

    public boolean isBackgroundProcessingEnabled() { return backgroundProcessingEnabled; }

    public Instant getProjectLivePaymentsDisputeEventsFromDate() {
        return projectLivePaymentsDisputeEventsFromDate;
    }

    public Instant getProjectTestPaymentsDisputeEventsFromDate() {
        return projectTestPaymentsDisputeEventsFromDate;
    }
}
