package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.tables.ColorRoleItemsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link ColorRoleItem} entity.
 */
public class ColorRoleItemRepository extends Repository<ColorRoleItem> {
    private static final ColorRoleItemsTable COLOR_ROLE_ITEMS = ColorRoleItemsTable.COLOR_ROLE_ITEMS;

    public ColorRoleItemRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<ColorRoleItem> getTable() {
        return COLOR_ROLE_ITEMS;
    }

    public ColorRoleItem findByRoleId(long roleId) {
        return ctx.selectFrom(COLOR_ROLE_ITEMS)
                .where(COLOR_ROLE_ITEMS.ROLE_ID.eq(roleId))
                .fetchOne();
    }
}