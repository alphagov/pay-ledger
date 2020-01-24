package uk.gov.pay.ledger.app;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.setup.Environment;
import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.dao.ResourceTypeDao;
import uk.gov.pay.ledger.metadatakey.dao.MetadataKeyDao;
import uk.gov.pay.ledger.report.dao.PerformanceReportDao;
import uk.gov.pay.ledger.report.dao.ReportDao;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transactionmetadata.dao.TransactionMetadataDao;

public class LedgerModule extends AbstractModule {
    private final LedgerConfig configuration;
    private final Environment environment;
    private final Jdbi jdbi;

    LedgerModule(
            LedgerConfig config,
            final Environment environment,
            final Jdbi jdbi
    ) {
        this.configuration = config;
        this.environment = environment;
        this.jdbi = jdbi;
    }

    @Override
    protected void configure() {
        bind(LedgerConfig.class).toInstance(configuration);
        bind(Environment.class).toInstance(environment);
    }

    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        ObjectMapper objectMapper = environment.getObjectMapper();
        objectMapper.findAndRegisterModules();

        return objectMapper;
    }

    @Provides
    @Singleton
    public EventDao provideEventDao() {
        return jdbi.onDemand(EventDao.class);
    }

    @Provides
    @Singleton
    public ResourceTypeDao provideResourceTypeDao() {
        return jdbi.onDemand(ResourceTypeDao.class);
    }

    @Provides
    @Singleton
    public TransactionDao provideTransactionDao() {
        return new TransactionDao(jdbi);
    }

    @Provides
    @Singleton
    public ReportDao provideReportDao() {
        return new ReportDao(jdbi);
    }

    @Provides
    @Singleton
    public PerformanceReportDao providePerformanceReportDao() {
        return new PerformanceReportDao(jdbi);
    }

    @Provides
    @Singleton
    public MetadataKeyDao provideMetadataKeyDao() {
        return jdbi.onDemand(MetadataKeyDao.class);
    }

    @Provides
    @Singleton
    public TransactionMetadataDao provideTransactionMetadataDao() {
        return jdbi.onDemand(TransactionMetadataDao.class);
    }

    @Provides
    public AmazonSQS sqsClient(LedgerConfig ledgerConfig) {
        AmazonSQSClientBuilder clientBuilder = AmazonSQSClientBuilder
                .standard();

        if (ledgerConfig.getSqsConfig().isNonStandardServiceEndpoint()) {

            BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(
                    ledgerConfig.getSqsConfig().getAccessKey(),
                    ledgerConfig.getSqsConfig().getSecretKey());

            clientBuilder
                    .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                    .withEndpointConfiguration(
                            new AwsClientBuilder.EndpointConfiguration(
                                    ledgerConfig.getSqsConfig().getEndpoint(),
                                    ledgerConfig.getSqsConfig().getRegion())
                    );
        } else {
            // uses AWS SDK's DefaultAWSCredentialsProviderChain to obtain credentials
            clientBuilder.withRegion(ledgerConfig.getSqsConfig().getRegion());
        }

        return clientBuilder.build();
    }
}
