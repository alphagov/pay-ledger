package uk.gov.pay.ledger.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.SQLException;

import static java.sql.DriverManager.getConnection;

public class PostgresTestDocker {

    private static final Logger logger = LoggerFactory.getLogger(PostgresTestDocker.class);

    private static final String DB_NAME = "ledger_test";
    private static final String DB_USERNAME = "test";
    private static final String DB_PASSWORD = "test";
    private static GenericContainer container;

    public static void getOrCreate() {
        try {
            if (container == null) {
                logger.info("Creating Postgres Container");

                container = new GenericContainer("postgres:11.1");
                container.addExposedPort(5432);

                container.addEnv("POSTGRES_USER", DB_USERNAME);
                container.addEnv("POSTGRES_PASSWORD", DB_PASSWORD);

                container.start();

                //todo: add DB health check
                Thread.sleep(5000);
                createDatabase(DB_NAME);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getConnectionUrl() {
        return "jdbc:postgresql://localhost:" + container.getMappedPort(5432) + "/";
    }

    public static void stopContainer() {
        container.stop();
        container = null;
    }

    private static void createDatabase(final String dbName) {
        final String dbUser = getDbUsername();

        try (Connection connection = getConnection(getConnectionUrl(), dbUser, getDbPassword())) {
            connection.createStatement().execute("CREATE DATABASE " + dbName + " WITH owner=" + dbUser + " TEMPLATE postgres");
            connection.createStatement().execute("GRANT ALL PRIVILEGES ON DATABASE " + dbName + " TO " + dbUser);
            connection.createStatement().execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static String getDbUri() {
        return getConnectionUrl() + DB_NAME;
    }

    public static String getDbPassword() {
        return DB_PASSWORD;
    }

    public static String getDbUsername() {
        return DB_USERNAME;
    }
}
