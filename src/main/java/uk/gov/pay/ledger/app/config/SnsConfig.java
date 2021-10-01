package uk.gov.pay.ledger.app.config;

import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;

public class SnsConfig extends Configuration {

    @NotNull
    private boolean snsEnabled;
    @NotNull
    private String region;
    private String cardPaymentEventsTopicArn;

    public boolean isSnsEnabled() {
        return snsEnabled;
    }

    public String getCardPaymentEventsTopicArn() {
        return cardPaymentEventsTopicArn;
    }

    public String getRegion() {
        return region;
    }
}
