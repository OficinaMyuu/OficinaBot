package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.ColorRoleItem;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

public class ColorRoleItemsTable extends InitializableTable<ColorRoleItem> {
    public static final ColorRoleItemsTable COLOR_ROLE_ITEMS = new ColorRoleItemsTable();

    public final Field<Integer> ID      = newField("id",         SQLDataType.INTEGER.identity(true));
    public final Field<Integer> PRICE   = newField("price",      SQLDataType.INTEGER.notNull());
    public final Field<Long> ROLE_ID    = newField("role_id",    SQLDataType.BIGINT.notNull());
    public final Field<Long> CREATED_AT = newField("created_at", SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT = newField("updated_at", SQLDataType.BIGINT.notNull());

    public ColorRoleItemsTable() {
        super("color_role_items");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .unique(ROLE_ID);
    }

    @NotNull
    @Override
    public Class<ColorRoleItem> getRecordType() {
        return ColorRoleItem.class;
    }
}