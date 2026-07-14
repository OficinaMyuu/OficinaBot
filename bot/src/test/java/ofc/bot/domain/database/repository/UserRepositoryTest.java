package ofc.bot.domain.database.repository;

import net.dv8tion.jda.api.entities.User;
import ofc.bot.domain.entity.AppUser;
import ofc.bot.testing.MySQLTestDatabase;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockExecuteContext;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryTest {
    @Test
    void fromUserCopiesBotFlag() {
        assertTrue(AppUser.fromUser(user(true)).isBot());
        assertFalse(AppUser.fromUser(user(false)).isBot());
    }

    @Test
    void upsertIncludesBotColumnAndValue() throws Exception {
        AtomicReference<MockExecuteContext> execution = new AtomicReference<>();
        try (MockConnection connection = new MockConnection(ctx -> {
            execution.set(ctx);
            return new MockResult[]{new MockResult(1)};
        })) {
            DSLContext ctx = DSL.using(connection, SQLDialect.MYSQL);
            new UserRepository(ctx).upsert(new AppUser(42L, "myuu", null, null, true, 1L, 1L));
        }

        MockExecuteContext captured = execution.get();
        assertNotNull(captured);
        assertTrue(captured.sql().contains("`is_bot`"));
        assertTrue(Arrays.asList(captured.bindings()).contains(true));
    }

    @Test
    void upsertPersistsAndUpdatesBotFlag() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            DSLContext ctx = MySQLTestDatabase.context(connection);
            ctx.execute("""
                    CREATE TABLE users (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        global_name VARCHAR(255),
                        avatar_hash VARCHAR(128),
                        is_bot BOOLEAN NOT NULL DEFAULT FALSE,
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL
                    )
                    """);
            UserRepository repository = new UserRepository(ctx);

            repository.upsert(new AppUser(42L, "myuu", null, null, false, 1L, 1L));
            assertFalse(repository.findById(42L).isBot());

            repository.upsert(new AppUser(42L, "myuu", null, null, true, 1L, 2L));
            assertTrue(repository.findById(42L).isBot());
        }
    }

    private static User user(boolean isBot) {
        return (User) Proxy.newProxyInstance(
                User.class.getClassLoader(),
                new Class<?>[]{User.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getIdLong" -> 42L;
                    case "getName" -> "myuu";
                    case "getGlobalName" -> "Oficina Myuu";
                    case "getAvatarId" -> "avatar-hash";
                    case "isBot" -> isBot;
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
