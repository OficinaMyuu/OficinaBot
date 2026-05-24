package ofc.bot.domain.entity;

import ofc.bot.domain.tables.FormerMembersRolesTable;

public class FormerMemberRole extends OficinaRecord<FormerMemberRole> {
    private static final FormerMembersRolesTable FORMER_MEMBER_ROLES = FormerMembersRolesTable.FORMER_MEMBERS_ROLES;

    public FormerMemberRole() {
        super(FORMER_MEMBER_ROLES);
    }

    public FormerMemberRole(long userId, long guildId, long roleId, boolean privileged, long createdAt) {
        this();
        set(FORMER_MEMBER_ROLES.USER_ID, userId);
        set(FORMER_MEMBER_ROLES.PRIVILEGED, privileged ? 1 : 0);
        set(FORMER_MEMBER_ROLES.GUILD_ID, guildId);
        set(FORMER_MEMBER_ROLES.ROLE_ID, roleId);
        set(FORMER_MEMBER_ROLES.CREATED_AT, createdAt);
    }

    public long getUserId() {
        return get(FORMER_MEMBER_ROLES.USER_ID);
    }

    public long getGuildId() {
        return get(FORMER_MEMBER_ROLES.GUILD_ID);
    }

    public long getRoleId() {
        return get(FORMER_MEMBER_ROLES.ROLE_ID);
    }

    public boolean isPrivileged() {
        Integer val = get(FORMER_MEMBER_ROLES.PRIVILEGED);
        return val != null && val != 0;
    }

    public long getTimeCreated() {
        return get(FORMER_MEMBER_ROLES.CREATED_AT);
    }

    public FormerMemberRole setUserId(long userId) {
        set(FORMER_MEMBER_ROLES.USER_ID, userId);
        return this;
    }

    public FormerMemberRole setPrivileged(boolean flag) {
        set(FORMER_MEMBER_ROLES.PRIVILEGED, flag ? 1 : 0);
        return this;
    }

    public FormerMemberRole setGuildId(long guildId) {
        set(FORMER_MEMBER_ROLES.GUILD_ID, guildId);
        return this;
    }

    public FormerMemberRole setRoleId(long roleId) {
        set(FORMER_MEMBER_ROLES.ROLE_ID, roleId);
        return this;
    }
}