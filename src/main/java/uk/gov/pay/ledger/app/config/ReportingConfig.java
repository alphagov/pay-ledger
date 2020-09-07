package uk.gov.pay.ledger.app.config;

import io.dropwizard.Configuration;

import javax.validation.Valid;

public class ReportingConfig extends Configuration {

    @Valid
    private int streamingCsvPageSize;

    public int getStreamingCsvPageSize() {
        return streamingCsvPageSize;
    }
}
