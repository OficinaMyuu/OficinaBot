package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.FormerMemberRole;
import ofc.bot.domain.tables.FormerMembersRolesTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;
import org.jooq.Condition;
import org.jooq.DSLContext;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.jooq.impl.DSL.noCondition;

/**
 * Repository for {@link FormerMemberRole} entity.
 */
public class FormerMemberRoleRepository extends Repository<FormerMemberRole> {
    private static final FormerMembersRolesTable FORMER_MEMBERS_ROLES = FormerMembersRolesTable.FORMER_MEMBERS_ROLES;

    public FormerMemberRoleRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<FormerMemberRole> getTable() {
        return FORMER_MEMBERS_ROLES;
    }

    /**
     * Deletes all backup roles before the provided period.
     * <p>
     * That is, making the following call:
     * <pre>
     *   {@code
     *   deleteBefore(30, TimeUnit.DAYS, true);
     *   }
     * </pre>
     * Will delete the backed up roles of all users (including privileged ones)
     * that were saved in a period longer than 30 days ago.
     * For instance, if someone left the guild 28 days ago, their roles will remain untouched.
     *
     * @param period the period of time in the past.
     * @param unit the unit of time used in the {@code period} parameter.
     * @param includePrivileged if privileged members should also have their backup deleted.
     * @return the amount of roles that were deleted.
     * @throws IllegalArgumentException if the {@code period} is negative.
     */
    public int deleteBefore(long period, TimeUnit unit, boolean includePrivileged) {
        long now = Bot.unixNow();
        long maxAge = now - unit.toSeconds(period);
        Condition deletePrivileged = includePrivileged
                ? noCondition()
                : FORMER_MEMBERS_ROLES.PRIVILEGED.eq(0);

        return ctx.deleteFrom(FORMER_MEMBERS_ROLES)
                .where(FORMER_MEMBERS_ROLES.CREATED_AT.lessThan(maxAge))
                .and(deletePrivileged)
                .execute();
    }

    @NotNull
    public List<FormerMemberRole> findByUserAndGuildId(long userId, long guildId) {
        return ctx.selectFrom(FORMER_MEMBERS_ROLES)
                .where(FORMER_MEMBERS_ROLES.GUILD_ID.eq(guildId))
                .and(FORMER_MEMBERS_ROLES.USER_ID.eq(userId))
                .fetch();
    }

    public void deleteByUserAndGuildId(long userId, long guildId) {
        ctx.deleteFrom(FORMER_MEMBERS_ROLES)
                .where(FORMER_MEMBERS_ROLES.USER_ID.eq(userId))
                .and(FORMER_MEMBERS_ROLES.GUILD_ID.eq(guildId))
                .execute();
    }
}