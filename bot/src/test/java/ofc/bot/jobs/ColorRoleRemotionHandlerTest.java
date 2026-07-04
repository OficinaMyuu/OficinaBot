package ofc.bot.jobs;

import ofc.bot.testing.MySQLTestDatabase;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.domain.database.repository.ColorRoleStateRepository;
import ofc.bot.domain.tables.ColorRolesStateTable;
import ofc.bot.handlers.economy.CurrencyType;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertNull;

class ColorRoleRemotionHandlerTest {
    @Test
    void shouldDeleteExpiredColorRoleRowWhenRoleNoLongerExists() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            DSLContext ctx = setup(connection);
            ColorRoleStateRepository repository = new ColorRoleStateRepository(ctx);
            ColorRoleState state = state();
            repository.save(state);

            new ColorRoleRemotionHandler().processExpiredColorRole(jdaWithGuildWithoutRole(), repository, state);

            assertNull(repository.findByUserAndRoleId(state.getUserId(), state.getRoleId()));
        }
    }

    private DSLContext setup(Connection connection) {
        DSLContext ctx = MySQLTestDatabase.context(connection);
        ColorRolesStateTable.COLOR_ROLES_STATES.getSchema(ctx).execute();
        return ctx;
    }

    private ColorRoleState state() {
        return new ColorRoleState(
                100,
                CurrencyType.OFICINA,
                10L,
                1L,
                100L,
                500L,
                1000L,
                1000L
        );
    }

    private JDA jdaWithGuildWithoutRole() {
        Guild guild = proxy(Guild.class, "getRoleById", null);
        return proxy(JDA.class, "getGuildById", guild);
    }

    private <T> T proxy(Class<T> type, String handledMethodName, Object returnValue) {
        Object proxy = Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (obj, method, args) -> method.getName().equals(handledMethodName)
                        ? returnValue
                        : defaultValue(method.getReturnType())
        );

        return type.cast(proxy);
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
