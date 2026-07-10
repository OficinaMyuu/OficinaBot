package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.StoreItemSettings;
import ofc.bot.domain.entity.enums.StoreItemType;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

import java.util.Arrays;

/**
 * Defines the durable, code-owned pricing configuration for group actions.
 */
public class StoreItemSettingsTable extends InitializableTable<StoreItemSettings> {
    public static final StoreItemSettingsTable STORE_ITEM_SETTINGS = new StoreItemSettingsTable();

    public final Field<String> ITEM_TYPE = newField("item_type", SQLDataType.VARCHAR(64).notNull());
    public final Field<Integer> PRICE = newField("price", SQLDataType.INTEGER.notNull());
    public final Field<Long> CREATED_AT = newField("created_at", SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT = newField("updated_at", SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_BY = newField("updated_by", SQLDataType.BIGINT);

    /**
     * Creates the table descriptor.
     */
    public StoreItemSettingsTable() {
        super("store_item_settings");
    }

    /**
     * Builds a test schema for repository integration tests.
     */
    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        String[] types = Arrays.stream(StoreItemType.values())
                .map(Enum::name)
                .toArray(String[]::new);

        return ctx.createTableIfNotExists(this)
                .primaryKey(ITEM_TYPE)
                .columns(fields())
                .check(ITEM_TYPE.in(types))
                .check(PRICE.ge(0));
    }

    /**
     * Returns the jOOQ record type mapped by this table.
     */
    @NotNull
    @Override
    public Class<StoreItemSettings> getRecordType() {
        return StoreItemSettings.class;
    }
}
