package io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Configuration for database connection pooling using HikariCP.
 * Provides high-performance connection management with configurable parameters.
 */
public class ConnectionPoolConfig {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolConfig.class);

    // Default pool configuration
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_MIN_IDLE = 2;
    private static final long DEFAULT_CONNECTION_TIMEOUT_MS = 30000; // 30 seconds
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 600000; // 10 minutes
    private static final long DEFAULT_MAX_LIFETIME_MS = 1800000; // 30 minutes
    private static final long DEFAULT_LEAK_DETECTION_THRESHOLD_MS = 60000; // 1 minute

    private ConnectionPoolConfig() {
        // Prevent instantiation
    }

    /**
     * Creates a HikariCP DataSource with default configuration.
     */
    public static DataSource createDataSource(String jdbcUrl, String username, String password, String poolName) {
        return createDataSource(
                jdbcUrl,
                username,
                password,
                poolName,
                DEFAULT_MAX_POOL_SIZE,
                DEFAULT_MIN_IDLE);
    }

    /**
     * Creates a HikariCP DataSource with custom pool size.
     */
    public static DataSource createDataSource(
            String jdbcUrl,
            String username,
            String password,
            String poolName,
            int maxPoolSize,
            int minIdle) {

        logger.info("Initializing HikariCP connection pool: {}", poolName);

        HikariConfig config = new HikariConfig();

        // Basic connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName(poolName);

        // Pool sizing
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);

        // Timeouts
        config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT_MS);
        config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT_MS);
        config.setMaxLifetime(DEFAULT_MAX_LIFETIME_MS);

        // Connection leak detection
        config.setLeakDetectionThreshold(DEFAULT_LEAK_DETECTION_THRESHOLD_MS);

        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        // Health check
        config.setConnectionTestQuery("SELECT 1");

        logger.info("Pool configuration - Max: {}, Min: {}, Timeout: {}ms",
                maxPoolSize, minIdle, DEFAULT_CONNECTION_TIMEOUT_MS);

        HikariDataSource dataSource = new HikariDataSource(config);

        logger.info("✅ Connection pool '{}' initialized successfully", poolName);

        return dataSource;
    }

    /**
     * Gracefully closes a HikariCP DataSource.
     */
    public static void closeDataSource(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDS = (HikariDataSource) dataSource;
            logger.info("Closing connection pool: {}", hikariDS.getPoolName());
            hikariDS.close();
            logger.info("✅ Connection pool closed successfully");
        }
    }
}
