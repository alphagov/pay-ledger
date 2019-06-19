package uk.gov.pay.ledger.rule;

import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.SQLException;

import static java.sql.DriverManager.getConnection;

public class PostgresTestDocker {
    private static final String DB_NAME = "ledger_test";
    private static PostgreSQLContainer container;

    static void getOrCreate() {
        try {
            if (container == null) {
                container = (PostgreSQLContainer) new PostgreSQLContainer("postgres:11.1")
                        .withUsername("test")
                        .withPassword("test");
                container.start();
                createDatabase(DB_NAME);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getConnectionUrl() {
        return container.getJdbcUrl();
    }

    private static void createDatabase(final String dbName) {
        final String dbUser = getDbUsername();

        try (Connection connection = getConnection(getDbRootUri(), dbUser, getDbPassword())) {
            connection.createStatement().execute("CREATE DATABASE " + dbName + " WITH owner=" + dbUser + " TEMPLATE postgres");
            connection.createStatement().execute("GRANT ALL PRIVILEGES ON DATABASE " + dbName + " TO " + dbUser);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getDbRootUri() {
        return container.getJdbcUrl();
    }

    static String getDbUri() {
        return getDbRootUri() + DB_NAME;
    }

    static String getDbPassword() {
        return container.getPassword();
    }

    static String getDbUsername() {
        return container.getUsername();
    }
}
