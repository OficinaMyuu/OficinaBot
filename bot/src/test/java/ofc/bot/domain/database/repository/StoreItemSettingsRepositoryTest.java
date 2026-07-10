package ofc.bot.domain.database.repository;

import ofc.bot.domain.entity.StoreItemSettings;
import ofc.bot.domain.entity.enums.StoreItemType;
import ofc.bot.domain.tables.StoreItemSettingsTable;
import ofc.bot.testing.MySQLTestDatabase;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreItemSettingsRepositoryTest {
    @Test
    void shouldReadConfiguredPricesWithoutFallbacks() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            StoreItemSettingsRepository repository = new StoreItemSettingsRepository(setup(connection));

            repository.save(new StoreItemSettings(StoreItemType.GROUP, 0, 100L));

            assertTrue(repository.findPrice(StoreItemType.GROUP).isPresent());
            assertEquals(0, repository.findPrice(StoreItemType.GROUP).getAsInt());
            assertFalse(repository.findPrice(StoreItemType.GROUP_SLOT).isPresent());
        }
    }

    @Test
    void shouldRejectInvalidSettingsRows() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            DSLContext ctx = setup(connection);
            StoreItemSettingsRepository repository = new StoreItemSettingsRepository(ctx);

            assertThrows(DataAccessException.class, () ->
                    repository.save(new StoreItemSettings(StoreItemType.GROUP, -1, 100L))
            );
            assertThrows(DataAccessException.class, () ->
                    ctx.insertInto(StoreItemSettingsTable.STORE_ITEM_SETTINGS)
                            .set(StoreItemSettingsTable.STORE_ITEM_SETTINGS.ITEM_TYPE, "NOT_A_STORE_ITEM")
                            .set(StoreItemSettingsTable.STORE_ITEM_SETTINGS.PRICE, 1)
                            .set(StoreItemSettingsTable.STORE_ITEM_SETTINGS.CREATED_AT, 100L)
                            .set(StoreItemSettingsTable.STORE_ITEM_SETTINGS.UPDATED_AT, 100L)
                            .execute()
            );
        }
    }

    private DSLContext setup(Connection connection) {
        DSLContext ctx = MySQLTestDatabase.context(connection);
        StoreItemSettingsTable.STORE_ITEM_SETTINGS.getSchema(ctx).execute();
        return ctx;
    }
}
