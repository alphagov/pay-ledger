package uk.gov.pay.ledger.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.SQLException;

import static java.sql.DriverManager.getConnection;

public class PostgresTestDocker {

    private static final Logger logger = LoggerFactory.getLogger(PostgresTestDocker.class);

    private static final String DB_NAME = "ledger_test";
    private static final String DB_USERNAME = "test";
    private static final String DB_PASSWORD = "test";
    private static PostgreSQLContainer<?> POSTGRES_CONTAINER;

    public static void getOrCreate() {
        try {
            if (POSTGRES_CONTAINER == null) {
                logger.info("Creating Postgres Container");

                POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:11.16")
                        .withUsername(DB_USERNAME)
                        .withPassword(DB_PASSWORD);

                POSTGRES_CONTAINER.start();
                createDatabase();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getConnectionUrl() {
        return POSTGRES_CONTAINER.getJdbcUrl();
    }

    public static void stopContainer() {
        POSTGRES_CONTAINER.stop();
        POSTGRES_CONTAINER = null;
    }

    private static void createDatabase() {
        try (Connection connection = getConnection(getConnectionUrl(), DB_USERNAME, getDbPassword())) {
            connection.createStatement().execute("CREATE DATABASE " + DB_NAME + " WITH owner=" + DB_USERNAME + " TEMPLATE postgres");
            connection.createStatement().execute("GRANT ALL PRIVILEGES ON DATABASE " + DB_NAME + " TO " + DB_USERNAME);
            connection.createStatement().execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getDbPassword() {
        return DB_PASSWORD;
    }

    public static String getDbUsername() {
        return DB_USERNAME;
    }
}
