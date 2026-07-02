package ofc.bot.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DB {
    private static final Logger LOGGER = LoggerFactory.getLogger(DB.class);
    private static final String DEFAULT_DATABASE_PORT = "3306";
    private static final String DEFAULT_DATABASE_NAME = "oficina_services";
    private static final String DEFAULT_DATABASE_COLLATION = "utf8mb4_unicode_ci";
    private static HikariDataSource dataSource;

    private DB() {}

    public static DSLContext context() {
        return DSL.using(dataSource, SQLDialect.MYSQL);
    }

    public static void init() {
        LOGGER.info("Initializing database...");

        HikariConfig config = new HikariConfig();
        DatabaseSettings settings = DatabaseSettings.fromEnv();

        config.setPoolName("oficina-registrar-mysql");
        config.setJdbcUrl(settings.jdbcUrl());
        config.setUsername(settings.user());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(settings.maxPoolSize());
        config.setMinimumIdle(settings.minIdle());
        config.setConnectionTimeout(settings.connectionTimeoutMs());
        config.setValidationTimeout(settings.validationTimeoutMs());
        config.setIdleTimeout(settings.idleTimeoutMs());
        config.setMaxLifetime(settings.maxLifetimeMs());
        config.setKeepaliveTime(settings.keepaliveTimeMs());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");

        dataSource = new HikariDataSource(config);
        context().fetchValue("SELECT 1");
        LOGGER.info("Created MySQL datasource for {}:{}", settings.host(), settings.port());
    }

    private record DatabaseSettings(
            String host,
            String port,
            String name,
            String user,
            String password,
            String collation,
            int maxPoolSize,
            int minIdle,
            long connectionTimeoutMs,
            long validationTimeoutMs,
            long idleTimeoutMs,
            long maxLifetimeMs,
            long keepaliveTimeMs
    ) {
        static DatabaseSettings fromEnv() {
            String host = requiredEnv("DATABASE_HOST");
            String port = env("DATABASE_PORT", DEFAULT_DATABASE_PORT);
            String name = env("DATABASE_NAME", DEFAULT_DATABASE_NAME);
            String user = requiredEnv("DATABASE_USER");
            String password = requiredEnv("DATABASE_PASSWORD");
            String collation = env("DATABASE_COLLATION", DEFAULT_DATABASE_COLLATION);
            int maxPoolSize = positiveIntEnv("DATABASE_MAX_POOL_SIZE", 3);
            int minIdle = Math.min(positiveIntEnv("DATABASE_MIN_IDLE", 1), maxPoolSize);

            return new DatabaseSettings(
                    host,
                    port,
                    name,
                    user,
                    password,
                    collation,
                    maxPoolSize,
                    minIdle,
                    positiveLongEnv("DATABASE_CONNECTION_TIMEOUT_MS", 10_000L),
                    positiveLongEnv("DATABASE_VALIDATION_TIMEOUT_MS", 5_000L),
                    positiveLongEnv("DATABASE_IDLE_TIMEOUT_MS", 600_000L),
                    positiveLongEnv("DATABASE_MAX_LIFETIME_MS", 1_500_000L),
                    positiveLongEnv("DATABASE_KEEPALIVE_TIME_MS", 300_000L)
            );
        }

        String jdbcUrl() {
            return "jdbc:mysql://%s:%s/%s?serverTimezone=UTC&connectionCollation=%s"
                    .formatted(host, port, name, collation);
        }
    }

    private static String requiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required database environment variable: " + key);
        }
        return value;
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int positiveIntEnv(String key, int fallback) {
        long parsed = positiveLongEnv(key, fallback);
        if (parsed > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(key + " must be less than or equal to " + Integer.MAX_VALUE);
        }
        return (int) parsed;
    }

    private static long positiveLongEnv(String key, long fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException(key + " must be positive");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a number", e);
        }
    }
}
