package uk.gov.pay.ledger.app.config;

import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ReportingConfig extends Configuration {

    @Valid
    private int maximumCsvRowsSize;

    public int getMaximumCsvRowsSize() {
        return maximumCsvRowsSize;
    }
}
