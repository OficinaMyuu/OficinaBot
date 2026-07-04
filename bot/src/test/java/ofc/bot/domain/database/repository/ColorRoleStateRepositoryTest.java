package ofc.bot.domain.database.repository;

import ofc.bot.testing.MySQLTestDatabase;

import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.domain.tables.ColorRolesStateTable;
import ofc.bot.handlers.economy.CurrencyType;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ColorRoleStateRepositoryTest {
    @Test
    void shouldFindExpiredColorRoles() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            DSLContext ctx = setup(connection);
            ColorRoleStateRepository repository = new ColorRoleStateRepository(ctx);

            repository.save(state(10L, 100L, 1000L, 500L));
            repository.save(state(20L, 200L, 1001L, 1500L));

            List<ColorRoleState> expired = repository.findExpired(1000L);

            assertEquals(1, expired.size());
            assertEquals(10L, expired.getFirst().getUserId());
            assertEquals(100L, expired.getFirst().getRoleId());
        }
    }

    @Test
    void shouldDeleteOnlyMatchingGuildUserAndRole() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            DSLContext ctx = setup(connection);
            ColorRoleStateRepository repository = new ColorRoleStateRepository(ctx);

            repository.save(state(10L, 100L, 1000L, 500L));
            repository.save(state(10L, 101L, 1001L, 500L));
            repository.save(state(11L, 100L, 1002L, 500L));

            assertEquals(1, repository.deleteByGuildUserAndRoleId(1L, 10L, 100L));

            assertNull(repository.findByUserAndRoleId(10L, 100L));
            assertNotNull(repository.findByUserAndRoleId(10L, 101L));
            assertNotNull(repository.findByUserAndRoleId(11L, 100L));
        }
    }

    private DSLContext setup(Connection connection) {
        DSLContext ctx = MySQLTestDatabase.context(connection);
        ColorRolesStateTable.COLOR_ROLES_STATES.getSchema(ctx).execute();
        return ctx;
    }

    private ColorRoleState state(long userId, long roleId, long createdAt, long expiresAt) {
        return new ColorRoleState(
                100,
                CurrencyType.OFICINA,
                userId,
                1L,
                roleId,
                expiresAt,
                createdAt,
                createdAt
        );
    }
}
