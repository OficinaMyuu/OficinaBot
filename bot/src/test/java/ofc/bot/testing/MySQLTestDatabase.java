package ofc.bot.testing;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Assumptions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

public final class MySQLTestDatabase {
    private static final String ENV_JDBC_URL = "OFICINA_TEST_MYSQL_JDBC_URL";
    private static final String ENV_USER = "OFICINA_TEST_MYSQL_USER";
    private static final String ENV_PASSWORD = "OFICINA_TEST_MYSQL_PASSWORD";

    private MySQLTestDatabase() {}

    public static Connection open() throws SQLException {
        String adminUrl = System.getenv(ENV_JDBC_URL);
        Assumptions.assumeTrue(
                adminUrl != null && !adminUrl.isBlank(),
                ENV_JDBC_URL + " is not set; skipping live MySQL integration test"
        );

        String schema = "oficina_test_" + UUID.randomUUID().toString().replace("-", "");
        Properties properties = connectionProperties();

        try (Connection admin = DriverManager.getConnection(adminUrl, properties)) {
            admin.createStatement().execute(
                    "CREATE DATABASE " + quoteIdentifier(schema) + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            );
        }

        Connection connection = DriverManager.getConnection(schemaUrl(adminUrl, schema), properties);
        return closeDroppingSchema(connection, adminUrl, properties, schema);
    }

    public static DSLContext context(Connection connection) {
        return DSL.using(connection, SQLDialect.MYSQL);
    }

    private static Properties connectionProperties() {
        Properties properties = new Properties();
        putIfPresent(properties, "user", ENV_USER);
        putIfPresent(properties, "password", ENV_PASSWORD);
        return properties;
    }

    private static void putIfPresent(Properties properties, String propertyName, String envName) {
        String value = System.getenv(envName);
        if (value != null && !value.isBlank()) {
            properties.setProperty(propertyName, value);
        }
    }

    private static Connection closeDroppingSchema(
            Connection connection,
            String adminUrl,
            Properties properties,
            String schema
    ) {
        InvocationHandler handler = new ConnectionInvocationHandler(connection, adminUrl, properties, schema);
        return (Connection) Proxy.newProxyInstance(
                MySQLTestDatabase.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler
        );
    }

    private static String schemaUrl(String jdbcUrl, String schema) {
        int queryIndex = jdbcUrl.indexOf('?');
        String withoutQuery = queryIndex >= 0 ? jdbcUrl.substring(0, queryIndex) : jdbcUrl;
        String query = queryIndex >= 0 ? jdbcUrl.substring(queryIndex) : "";

        String prefix = "jdbc:mysql://";
        int pathIndex = withoutQuery.indexOf('/', prefix.length());
        String hostPart = pathIndex >= 0 ? withoutQuery.substring(0, pathIndex) : withoutQuery;
        return hostPart + "/" + schema + query;
    }

    private static String quoteIdentifier(String value) {
        return "`" + value.replace("`", "``") + "`";
    }

    private record ConnectionInvocationHandler(
            Connection delegate,
            String adminUrl,
            Properties properties,
            String schema
    ) implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!method.getName().equals("close")) {
                return method.invoke(delegate, args);
            }

            SQLException failure = null;
            try {
                delegate.close();
            } catch (SQLException e) {
                failure = e;
            }

            try (Connection admin = DriverManager.getConnection(adminUrl, properties)) {
                admin.createStatement().execute("DROP DATABASE IF EXISTS " + quoteIdentifier(schema));
            } catch (SQLException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }

            if (failure != null) {
                throw failure;
            }
            return null;
        }
    }
}
