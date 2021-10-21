package uk.gov.pay.ledger.app.config;

import io.dropwizard.Configuration;

import javax.validation.Valid;

public class ReportingConfig extends Configuration {

    @Valid
    private int streamingCsvPageSize;

    @Valid
    private int searchQueryTimeoutInSeconds;

    public int getStreamingCsvPageSize() {
        return streamingCsvPageSize;
    }

    public int getSearchQueryTimeoutInSeconds() {
        return searchQueryTimeoutInSeconds;
    }
}