package ofc.bot.domain.database.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.StoreItemSettings;
import ofc.bot.domain.entity.enums.StoreItemType;
import ofc.bot.domain.tables.StoreItemSettingsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.OptionalInt;

/**
 * Reads the mutable prices for code-owned group actions.
 */
public class StoreItemSettingsRepository extends Repository<StoreItemSettings> {
    private static final StoreItemSettingsTable STORE_ITEM_SETTINGS =
            StoreItemSettingsTable.STORE_ITEM_SETTINGS;

    /**
     * Creates a repository backed by the provided jOOQ context.
     */
    public StoreItemSettingsRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    /**
     * Finds the configured price for an action without providing a code fallback.
     */
    @NotNull
    public OptionalInt findPrice(@NotNull StoreItemType itemType) {
        Integer price = ctx.select(STORE_ITEM_SETTINGS.PRICE)
                .from(STORE_ITEM_SETTINGS)
                .where(STORE_ITEM_SETTINGS.ITEM_TYPE.eq(itemType.name()))
                .fetchOne(STORE_ITEM_SETTINGS.PRICE);
        return price == null ? OptionalInt.empty() : OptionalInt.of(price);
    }

    /**
     * Returns the table owned by this repository.
     */
    @NotNull
    @Override
    public InitializableTable<StoreItemSettings> getTable() {
        return STORE_ITEM_SETTINGS;
    }
}
