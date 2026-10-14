package com.hms.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Cloud-ready database connection provider using HikariCP connection pooling
 * combined with Amazon RDS Proxy.
 *
 * Cloud Readiness Fix (cr-java-0073 - Direct JDBC Connections):
 * Replaces the raw DriverManager.getConnection() approach with a HikariCP
 * DataSource. All connection parameters are read from environment variables
 * so that no credentials or endpoint addresses are hard-coded in source code.
 *
 * Cloud Readiness Fix (cr-java-0097 - Missing Connection Timeouts):
 * Adds explicit connection timeout, socket timeout, login timeout, and
 * network-level JDBC driver timeouts to prevent indefinite hangs and
 * resource exhaustion in cloud environments with variable network latency.
 *
 * Required environment variables (set in ECS task definition / Elastic
 * Beanstalk environment / EC2 user-data):
 *
 *   DB_URL      – JDBC URL pointing to the Amazon RDS Proxy endpoint, e.g.
 *                 jdbc:mysql://<rds-proxy-endpoint>:3306/hospital
 *   DB_USERNAME – Database username
 *   DB_PASSWORD – Database password (or retrieve via AWS Secrets Manager)
 *
 * HikariCP pool tuning can be adjusted via the constants below or promoted
 * to additional environment variables as needed.
 */
public class DBConnection {

    // ---------------------------------------------------------------------------
    // Pool configuration – read from environment variables with safe defaults
    // ---------------------------------------------------------------------------

    /** JDBC URL for the Amazon RDS Proxy endpoint. */
    private static final String DB_URL =
            getEnv("DB_URL", "jdbc:mysql://localhost:3306/hospital");

    /** Database username. */
    private static final String DB_USERNAME =
            getEnv("DB_USERNAME", "root");

    /**
     * Database password.
     * In production this should be sourced from AWS Secrets Manager via
     * {@link com.hms.util.AwsSecretsManagerUtil#getSecret(String)}.
     */
    private static final String DB_PASSWORD =
            getEnv("DB_PASSWORD", "");

    // ---------------------------------------------------------------------------
    // HikariCP pool sizing defaults (tune per workload)
    // ---------------------------------------------------------------------------
    private static final int  POOL_MINIMUM_IDLE        = 5;
    private static final int  POOL_MAXIMUM_SIZE        = 20;

    // ---------------------------------------------------------------------------
    // Timeout configuration (cr-java-0097 - Missing Connection Timeouts)
    // All timeout values are in milliseconds unless noted otherwise.
    // ---------------------------------------------------------------------------

    /**
     * Maximum time HikariCP will wait to obtain a connection from the pool
     * before throwing a {@link java.sql.SQLTimeoutException}.
     * Prevents threads from blocking indefinitely when the pool is exhausted.
     */
    private static final long CONNECTION_TIMEOUT_MS    = 30_000L;  // 30 s

    /**
     * Maximum time a connection may sit idle in the pool before being evicted.
     */
    private static final long IDLE_TIMEOUT_MS          = 600_000L; // 10 min

    /**
     * Maximum lifetime of a connection in the pool regardless of activity.
     * Should be set shorter than the database's wait_timeout to avoid stale
     * connections being handed out.
     */
    private static final long MAX_LIFETIME_MS          = 1_800_000L; // 30 min

    /**
     * Maximum time HikariCP will wait for a new connection to be established
     * (i.e. the physical TCP + authentication handshake).
     * Maps to the JDBC driver's {@code connectTimeout} property.
     */
    private static final int  JDBC_CONNECT_TIMEOUT_SEC = 10;  // 10 s

    /**
     * Maximum time the JDBC driver will wait for data on an established socket
     * before raising a {@link java.net.SocketTimeoutException}.
     * Maps to the JDBC driver's {@code socketTimeout} property.
     * Prevents queries from hanging indefinitely due to network issues.
     */
    private static final int  JDBC_SOCKET_TIMEOUT_SEC  = 30;  // 30 s

    /**
     * Maximum time HikariCP will wait for a connection validation query
     * ({@code SELECT 1}) to complete.
     */
    private static final int  VALIDATION_TIMEOUT_SEC   = 5;   // 5 s

    // ---------------------------------------------------------------------------
    // Singleton DataSource – initialised once at class-load time
    // ---------------------------------------------------------------------------
    private static final HikariDataSource DATA_SOURCE;

    static {
        HikariConfig config = new HikariConfig();

        // JDBC driver – MySQL Connector/J 8.x includes the cj driver class
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Connection target: Amazon RDS Proxy endpoint (or local for dev)
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USERNAME);
        config.setPassword(DB_PASSWORD);

        // Pool sizing
        config.setMinimumIdle(POOL_MINIMUM_IDLE);
        config.setMaximumPoolSize(POOL_MAXIMUM_SIZE);

        // ---------------------------------------------------------------------------
        // Timeout settings (cr-java-0097 - Missing Connection Timeouts)
        // ---------------------------------------------------------------------------

        // HikariCP-level: max wait to borrow a connection from the pool
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);

        // HikariCP-level: idle eviction and max lifetime
        config.setIdleTimeout(IDLE_TIMEOUT_MS);
        config.setMaxLifetime(MAX_LIFETIME_MS);

        // HikariCP-level: max time to validate a connection before returning it
        config.setValidationTimeout(VALIDATION_TIMEOUT_SEC * 1_000L);

        // JDBC driver-level: TCP connect timeout (prevents hanging on unreachable host)
        config.addDataSourceProperty("connectTimeout",
                String.valueOf(JDBC_CONNECT_TIMEOUT_SEC * 1_000));

        // JDBC driver-level: socket read timeout (prevents hanging on slow/stalled queries)
        config.addDataSourceProperty("socketTimeout",
                String.valueOf(JDBC_SOCKET_TIMEOUT_SEC * 1_000));

        // MySQL Connector/J 8.x: enable TCP keep-alive to detect dead connections
        config.addDataSourceProperty("tcpKeepAlive", "true");

        // Pool name for monitoring / CloudWatch metrics
        config.setPoolName("HmsHikariPool");

        // Recommended for MySQL: validate connections before use
        config.setConnectionTestQuery("SELECT 1");

        // RDS Proxy / Aurora: disable auto-commit per connection to allow
        // explicit transaction management in DAO layer
        config.setAutoCommit(true);

        DATA_SOURCE = new HikariDataSource(config);
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /**
     * Returns a {@link Connection} from the HikariCP pool.
     *
     * <p>Callers are responsible for closing the connection (which returns it
     * to the pool) – preferably via try-with-resources:
     * <pre>
     *   try (Connection conn = DBConnection.getConn()) {
     *       // use conn
     *   }
     * </pre>
     *
     * @return a pooled {@link Connection}
     * @throws RuntimeException if a connection cannot be obtained from the pool
     */
    public static Connection getConn() {
        try {
            return DATA_SOURCE.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to obtain a database connection from HikariCP pool: " + e.getMessage(), e);
        }
    }

    /**
     * Closes the underlying HikariCP pool.  Call this during application
     * shutdown (e.g. from a ServletContextListener#contextDestroyed).
     */
    public static void closePool() {
        if (DATA_SOURCE != null && !DATA_SOURCE.isClosed()) {
            DATA_SOURCE.close();
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /**
     * Reads an environment variable, returning {@code defaultValue} when the
     * variable is absent or blank.
     */
    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }
}
