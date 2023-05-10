package uk.gov.pay.ledger.app.config;

import io.dropwizard.Configuration;

import java.net.URI;

import javax.validation.constraints.NotNull;

public class SnsConfig extends Configuration {

    @NotNull
    private boolean snsEnabled;
    @NotNull
    private String region;
    private String accessKey;
    private String secretKey;
    private String cardPaymentEventsTopicArn;
    private String cardPaymentDisputeEventsTopicArn;
    @NotNull
    private boolean publishCardPaymentEventsToSns;
    @NotNull
    private boolean publishCardPaymentDisputeEventsToSns;

    private boolean nonStandardServiceEndpoint;
    private URI endpoint;

    public boolean isSnsEnabled() {
        return snsEnabled;
    }

    public String getRegion() {
        return region;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public boolean isNonStandardServiceEndpoint() {
        return nonStandardServiceEndpoint;
    }

    public String getCardPaymentEventsTopicArn() {
        return cardPaymentEventsTopicArn;
    }

    public String getCardPaymentDisputeEventsTopicArn() {
        return cardPaymentDisputeEventsTopicArn;
    }

    public boolean isPublishCardPaymentEventsToSns() {
        return publishCardPaymentEventsToSns;
    }

    public boolean isPublishCardPaymentDisputeEventsToSns() {
        return publishCardPaymentDisputeEventsToSns;
    }
}
