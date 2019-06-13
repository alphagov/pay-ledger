package uk.gov.pay.ledger.app.config;

import io.dropwizard.Configuration;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

public class SqsConfig extends Configuration {

    @NotNull
    private String eventQueueUrl;

    @NotNull
    private String region;
    private String accessKey;
    private String secretKey;

    @Max(20)
    private int messageMaximumWaitTimeInSeconds;
    @Max(10)
    private int messageMaximumBatchSize;


    private boolean nonStandardServiceEndpoint;
    private String endpoint;

    public String getEventQueueUrl() {
        return eventQueueUrl;
    }

    public int getMessageMaximumBatchSize() {
        return messageMaximumBatchSize;
    }

    public int getMessageMaximumWaitTimeInSeconds() {
        return messageMaximumWaitTimeInSeconds;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getRegion() {
        return region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public boolean isNonStandardServiceEndpoint() {
        return nonStandardServiceEndpoint;
    }
}
