package uk.gov.pay.ledger.app;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.setup.Environment;
import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.dao.ResourceTypeDao;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;

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
        return jdbi.onDemand(TransactionDao.class);
    }
}
