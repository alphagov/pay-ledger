package uk.gov.pay.ledger.app.config;

import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;

public class SnsConfig extends Configuration {

    @NotNull
    private boolean snsEnabled;
    @NotNull
    private String region;
    private String cardPaymentEventsTopicArn;
    private String cardPaymentDisputeEventsTopicArn;
    @NotNull
    private boolean publishCardPaymentEventsToSns;
    @NotNull
    private boolean publishCardPaymentDisputeEventsToSns;

    public boolean isSnsEnabled() {
        return snsEnabled;
    }

    public String getRegion() {
        return region;
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
