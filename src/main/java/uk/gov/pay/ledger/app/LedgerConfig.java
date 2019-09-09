package uk.gov.pay.ledger.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import uk.gov.pay.ledger.app.config.QueueMessageReceiverConfig;
import uk.gov.pay.ledger.app.config.SqsConfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
    @JsonProperty("queueMessageReceiverConfig")
    private QueueMessageReceiverConfig queueMessageReceiverConfig;

    public SqsConfig getSqsConfig() {
        return sqsConfig;
    }

    public QueueMessageReceiverConfig getQueueMessageReceiverConfig() {
        return queueMessageReceiverConfig;
    }
}
