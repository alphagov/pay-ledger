package uk.gov.pay.ledger.app.config;

import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;

public class ExpungeOrRedactHistoricalDataConfig extends Configuration {

    @NotNull
    private boolean expungeAndRedactHistoricalDataEnabled;
    @NotNull
    private int expungeOrRedactDataOlderThanDays;
    @NotNull
    private int noOfTransactionsToRedact;

    public boolean isExpungeAndRedactHistoricalDataEnabled() {
        return expungeAndRedactHistoricalDataEnabled;
    }

    public int getExpungeOrRedactDataOlderThanDays() {
        return expungeOrRedactDataOlderThanDays;
    }

    public int getNoOfTransactionsToRedact() {
        return noOfTransactionsToRedact;
    }
}
