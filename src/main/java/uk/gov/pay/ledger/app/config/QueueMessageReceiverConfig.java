package uk.gov.pay.ledger.app.config;

import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class QueueMessageReceiverConfig extends Configuration {

    @Valid
    @NotNull
    private int threadDelayInSeconds;

    @Valid
    @NotNull
    private int numberOfThreads;

    public int getThreadDelayInSeconds() {
        return threadDelayInSeconds;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }
}
