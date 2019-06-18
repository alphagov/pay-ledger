package uk.gov.pay.ledger.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresTestDocker {

    private static final Logger logger = LoggerFactory.getLogger(PostgresTestDocker.class);

    private static PostgreSQLContainer postgreSQLContainer;

    public static PostgreSQLContainer initialise() {
        try {
            createContainer();
            return postgreSQLContainer;
        } catch (Exception e) {
            logger.error("Exception initialising Postgres Container - {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    private static void createContainer() {
        if (postgreSQLContainer == null) {
            postgreSQLContainer = new PostgreSQLContainer("postgres:11.1");
            postgreSQLContainer.start();
        }
    }
}
