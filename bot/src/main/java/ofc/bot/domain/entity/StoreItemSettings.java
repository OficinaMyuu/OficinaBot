package ofc.bot.domain.entity;

import ofc.bot.domain.entity.enums.StoreItemType;
import ofc.bot.domain.tables.StoreItemSettingsTable;
import org.jetbrains.annotations.NotNull;

/**
 * Stores the mutable price for one code-owned {@link StoreItemType}.
 */
public class StoreItemSettings extends OficinaRecord<StoreItemSettings> {
    private static final StoreItemSettingsTable STORE_ITEM_SETTINGS =
            StoreItemSettingsTable.STORE_ITEM_SETTINGS;

    /**
     * Creates an empty jOOQ record instance for fetch mapping.
     */
    public StoreItemSettings() {
        super(STORE_ITEM_SETTINGS);
    }

    /**
     * Creates a settings record for repository integration tests.
     */
    public StoreItemSettings(@NotNull StoreItemType itemType, int price, long createdAt) {
        this();
        set(STORE_ITEM_SETTINGS.ITEM_TYPE, itemType.name());
        set(STORE_ITEM_SETTINGS.PRICE, price);
        set(STORE_ITEM_SETTINGS.CREATED_AT, createdAt);
        set(STORE_ITEM_SETTINGS.UPDATED_AT, createdAt);
    }

    /**
     * Returns the static action identity represented by this row.
     */
    @NotNull
    public StoreItemType getItemType() {
        return StoreItemType.valueOf(get(STORE_ITEM_SETTINGS.ITEM_TYPE));
    }

    /**
     * Returns the configured action price. Zero means the action is free.
     */
    public int getPrice() {
        return get(STORE_ITEM_SETTINGS.PRICE);
    }

    /**
     * Returns when this settings row was created.
     */
    public long getTimeCreated() {
        return get(STORE_ITEM_SETTINGS.CREATED_AT);
    }

    /**
     * Returns when this settings row was most recently updated.
     */
    @Override
    public long getLastUpdated() {
        return get(STORE_ITEM_SETTINGS.UPDATED_AT);
    }

    /**
     * Returns the dashboard user who most recently changed the value, if any.
     */
    public Long getUpdatedBy() {
        return get(STORE_ITEM_SETTINGS.UPDATED_BY);
    }
}
