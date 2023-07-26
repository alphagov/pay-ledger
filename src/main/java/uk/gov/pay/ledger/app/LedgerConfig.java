package uk.gov.pay.ledger.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import uk.gov.pay.ledger.app.config.QueueMessageReceiverConfig;
import uk.gov.pay.ledger.app.config.ReportingConfig;
import uk.gov.pay.ledger.app.config.SnsConfig;
import uk.gov.pay.ledger.app.config.SqsConfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Optional;

public class LedgerConfig extends Configuration {
    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    @JsonProperty("database")
    public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
        this.database = dataSourceFactory;
    }

    @NotNull
    @JsonProperty("sqsConfig")
    private SqsConfig sqsConfig;

    @NotNull
    @JsonProperty("snsConfig")
    private SnsConfig snsConfig;

    @NotNull
    @JsonProperty("queueMessageReceiverConfig")
    private QueueMessageReceiverConfig queueMessageReceiverConfig;

    @NotNull
    @JsonProperty("reportingConfig")
    private ReportingConfig reportingConfig;

    @JsonProperty("ecsContainerMetadataUriV4")
    private URI ecsContainerMetadataUriV4;

    public SqsConfig getSqsConfig() {
        return sqsConfig;
    }

    public SnsConfig getSnsConfig() {
        return snsConfig;
    }

    public QueueMessageReceiverConfig getQueueMessageReceiverConfig() {
        return queueMessageReceiverConfig;
    }

    public ReportingConfig getReportingConfig() {
        return reportingConfig;
    }

    public Optional<URI> getEcsContainerMetadataUriV4() {
        return Optional.ofNullable(ecsContainerMetadataUriV4);
    }
}
