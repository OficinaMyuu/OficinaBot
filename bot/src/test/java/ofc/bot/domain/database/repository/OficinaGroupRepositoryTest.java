package ofc.bot.domain.database.repository;

import ofc.bot.testing.MySQLTestDatabase;

import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.RentStatus;
import ofc.bot.domain.tables.OficinaGroupsTable;
import ofc.bot.domain.tables.UsersTable;
import ofc.bot.handlers.economy.CurrencyType;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static ofc.bot.domain.tables.UsersTable.USERS;
import static org.junit.jupiter.api.Assertions.*;

class OficinaGroupRepositoryTest {
    @Test
    void shouldFindExistingEmoji() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            OficinaGroupRepository repository = new OficinaGroupRepository(setup(connection));

            repository.save(group(10L, "Piratas", "🏴"));

            assertTrue(repository.existsByEmoji("🏴"));
            assertFalse(repository.existsByEmoji("⚓"));
        }
    }

    @Test
    void shouldIgnoreCurrentGroupWhenCheckingEmojiCollision() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            OficinaGroupRepository repository = new OficinaGroupRepository(setup(connection));
            repository.save(group(10L, "Piratas", "🏴"));
            repository.save(group(20L, "Marujos", "⚓"));

            OficinaGroup pirates = repository.findByOwnerId(10L);

            assertFalse(repository.existsByEmojiExceptId("🏴", pirates.getId()));
            assertTrue(repository.existsByEmojiExceptId("⚓", pirates.getId()));
        }
    }

    private DSLContext setup(Connection connection) {
        DSLContext ctx = MySQLTestDatabase.context(connection);
        UsersTable.USERS.getSchema(ctx).execute();
        OficinaGroupsTable.OFICINA_GROUPS.getSchema(ctx).execute();
        saveUser(ctx, 10L);
        saveUser(ctx, 20L);
        return ctx;
    }

    private void saveUser(DSLContext ctx, long id) {
        ctx.insertInto(USERS)
                .set(USERS.ID, id)
                .set(USERS.NAME, "user-" + id)
                .set(USERS.CREATED_AT, 100L)
                .set(USERS.UPDATED_AT, 100L)
                .execute();
    }

    private OficinaGroup group(long ownerId, String name, String emoji) {
        return new OficinaGroup(name, ownerId, 1L, RentStatus.FREE, false)
                .setRoleId(ownerId + 100L)
                .setEmoji(emoji)
                .setRoleEmoji(false)
                .setCurrency(CurrencyType.OFICINA)
                .setAmountPaid(0)
                .setInvoiceAmount(0)
                .setRefundPercent(0D)
                .setTimeCreated(100L)
                .setLastUpdated(100L);
    }
}
