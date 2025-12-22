package ofc.bot.domain.entity;

import ofc.bot.domain.tables.ColorRoleItemsTable;

public class ColorRoleItem extends OficinaRecord<ColorRoleItem> {
    private static final ColorRoleItemsTable COLOR_ROLE_ITEMS = ColorRoleItemsTable.COLOR_ROLE_ITEMS;

    public ColorRoleItem() {
        super(COLOR_ROLE_ITEMS);
    }

    public int getId() {
        return get(COLOR_ROLE_ITEMS.ID);
    }

    public int getPrice() {
        return get(COLOR_ROLE_ITEMS.PRICE);
    }

    public long getRoleId() {
        return get(COLOR_ROLE_ITEMS.ROLE_ID);
    }

    public long getTimeCreated() {
        return get(COLOR_ROLE_ITEMS.CREATED_AT);
    }

    @Override
    public long getLastUpdated() {
        return get(COLOR_ROLE_ITEMS.UPDATED_AT);
    }
}