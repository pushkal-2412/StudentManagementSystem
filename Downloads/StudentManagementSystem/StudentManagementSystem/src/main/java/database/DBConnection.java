package database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Central JDBC connection provider.
 *
 * <p>Credentials should be supplied through environment variables
 * (DB_URL, DB_USERNAME, DB_PASSWORD). Legacy SMS_DB_* overrides remain
 * supported for deployments migrating from the previous configuration.</p>
 */
public final class DBConnection {

    private static final Properties PROPS = new Properties();
    private static String url;
    private static String user;
    private static String password;

    static {
        try (InputStream in = DBConnection.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "application.properties not found on the classpath.");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read application.properties", e);
        }

        url = env("DB_URL", env("SMS_DB_URL", PROPS.getProperty("db.url")));
        user = env("DB_USERNAME", env("SMS_DB_USER", PROPS.getProperty("db.user")));
        password = env("DB_PASSWORD", env("SMS_DB_PASSWORD", PROPS.getProperty("db.password")));

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "MySQL JDBC driver not found. Run 'mvn clean package' so the "
                  + "com.mysql:mysql-connector-j dependency is on the classpath.", e);
        }
    }

    private DBConnection() { }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    /** @return a new physical connection. The caller closes it (try-with-resources). */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Throws a readable exception when the database is unreachable. */
    public static void testConnection() throws SQLException {
        try (Connection c = getConnection()) {
            if (!c.isValid(5)) {
                throw new SQLException("Database connection is not valid.");
            }
        }
    }

    /** Short description used in the status bar. */
    public static String describeTarget() {
        String safe = url == null ? "not configured" : url;
        int q = safe.indexOf('?');
        if (q > 0) {
            safe = safe.substring(0, q);
        }
        return safe + "  (user: " + user + ")";
    }

    public static String getProperty(String key, String fallback) {
        return PROPS.getProperty(key, fallback);
    }

    /** Prints the configured target and connectivity hint to the console at startup. */
    public static void logStartupInfo() {
        System.out.println("[SMS] Target database : " + describeTarget());
        try {
            testConnection();
            System.out.println("[SMS] Database connection OK.");
        } catch (SQLException ex) {
            System.out.println("[SMS] Database unreachable: " + ex.getMessage());
            System.out.println("[SMS] Check that MySQL 8 is running and that "
                    + "src/main/resources/application.properties is correct.");
        }
    }
}
