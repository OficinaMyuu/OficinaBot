package ofc.bot.domain.entity;

import ofc.bot.domain.tables.ColorRolesStateTable;
import ofc.bot.handlers.economy.CurrencyType;
import org.jetbrains.annotations.NotNull;

public class ColorRoleState extends OficinaRecord<ColorRoleState> {
    private static final ColorRolesStateTable COLOR_ROLES_STATES = ColorRolesStateTable.COLOR_ROLES_STATES;

    public ColorRoleState() {
        super(COLOR_ROLES_STATES);
    }

    public ColorRoleState(
            int valuePaid, CurrencyType currency,
            long userId, long guildId, long roleId,
            long createdAt, long updatedAt)
    {
        this();
        set(COLOR_ROLES_STATES.VALUE_PAID, valuePaid);
        set(COLOR_ROLES_STATES.CURRENCY, currency.name());
        set(COLOR_ROLES_STATES.USER_ID, userId);
        set(COLOR_ROLES_STATES.GUILD_ID, guildId);
        set(COLOR_ROLES_STATES.ROLE_ID, roleId);
        set(COLOR_ROLES_STATES.CREATED_AT, createdAt);
        set(COLOR_ROLES_STATES.UPDATED_AT, updatedAt);
    }

    public int getId() {
        return get(COLOR_ROLES_STATES.ID);
    }

    public int getValuePaid() {
        return get(COLOR_ROLES_STATES.VALUE_PAID);
    }

    public CurrencyType getCurrency() {
        String type = get(COLOR_ROLES_STATES.CURRENCY);
        return CurrencyType.valueOf(type);
    }

    public long getUserId() {
        return get(COLOR_ROLES_STATES.USER_ID);
    }

    public long getGuildId() {
        return get(COLOR_ROLES_STATES.GUILD_ID);
    }

    public long getRoleId() {
        return get(COLOR_ROLES_STATES.ROLE_ID);
    }

    public long getTimeCreated() {
        return get(COLOR_ROLES_STATES.CREATED_AT);
    }

    public long getLastUpdated() {
        return get(COLOR_ROLES_STATES.UPDATED_AT);
    }

    public ColorRoleState setUserId(long userId) {
        set(COLOR_ROLES_STATES.USER_ID, userId);
        return this;
    }

    public ColorRoleState setGuildId(long guildId) {
        set(COLOR_ROLES_STATES.GUILD_ID, guildId);
        return this;
    }

    public ColorRoleState setRoleId(long roleId) {
        set(COLOR_ROLES_STATES.ROLE_ID, roleId);
        return this;
    }

    @NotNull
    public ColorRoleState setLastUpdated(long updatedAt) {
        set(COLOR_ROLES_STATES.UPDATED_AT, updatedAt);
        return this;
    }
}