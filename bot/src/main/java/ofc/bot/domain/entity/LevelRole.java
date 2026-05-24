package ofc.bot.domain.entity;

import net.dv8tion.jda.api.entities.Role;
import ofc.bot.Main;
import ofc.bot.domain.tables.LevelsRolesTable;

public class LevelRole extends OficinaRecord<LevelRole> {
    private static final LevelsRolesTable LEVELS_ROLES = LevelsRolesTable.LEVELS_ROLES;

    public LevelRole() {
        super(LEVELS_ROLES);
    }

    public int getId() {
        return get(LEVELS_ROLES.ID);
    }

    public int getLevel() {
        return get(LEVELS_ROLES.LEVEL);
    }

    public long getRoleId() {
        return get(LEVELS_ROLES.ROLE_ID);
    }

    public long getTimeCreated() {
        return get(LEVELS_ROLES.CREATED_AT);
    }

    public Role toRole() {
        return Main.getApi().getRoleById(getRoleId());
    }

    public int getColor() {
        Role role = toRole();
        return role == null ? 0 : role.getColorRaw();
    }
}