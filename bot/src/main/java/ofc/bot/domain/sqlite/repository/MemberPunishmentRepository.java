package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.MemberPunishment;
import ofc.bot.domain.tables.MembersPunishmentsTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

import static org.jooq.impl.DSL.noCondition;

/**
 * Repository for {@link MemberPunishment} entity.
 */
public class MemberPunishmentRepository extends Repository<MemberPunishment> {
    private static final MembersPunishmentsTable MEMBERS_PUNISHMENTS = MembersPunishmentsTable.MEMBERS_PUNISHMENTS;

    public MemberPunishmentRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<MemberPunishment> getTable() {
        return MEMBERS_PUNISHMENTS;
    }

    public MemberPunishment findById(int id) {
        return ctx.selectFrom(MEMBERS_PUNISHMENTS)
                .where(MEMBERS_PUNISHMENTS.ID.eq(id))
                .and(MEMBERS_PUNISHMENTS.ACTIVE.eq(true))
                .fetchOne();
    }

    public List<MemberPunishment> findByUserAndGuildId(long userId, long guildId, int limit) {
        return ctx.selectFrom(MEMBERS_PUNISHMENTS)
                .where(MEMBERS_PUNISHMENTS.USER_ID.eq(userId))
                .and(MEMBERS_PUNISHMENTS.GUILD_ID.eq(guildId))
                .and(MEMBERS_PUNISHMENTS.ACTIVE.eq(true))
                .orderBy(MEMBERS_PUNISHMENTS.CREATED_AT.desc())
                .limit(limit)
                .fetch();
    }

    public List<MemberPunishment> findByUserAndGuildId(
            long userId, long guildId, int limit, int offset, boolean showInactive
    ) {
        var activeCond = showInactive ? noCondition() : MEMBERS_PUNISHMENTS.ACTIVE.eq(true);
        return ctx.selectFrom(MEMBERS_PUNISHMENTS)
                .where(MEMBERS_PUNISHMENTS.USER_ID.eq(userId))
                .and(MEMBERS_PUNISHMENTS.GUILD_ID.eq(guildId))
                .and(activeCond)
                .orderBy(MEMBERS_PUNISHMENTS.CREATED_AT.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public int countByUserAndGuildId(long userId, long guildId) {
        return ctx.fetchCount(MEMBERS_PUNISHMENTS,
                MEMBERS_PUNISHMENTS.GUILD_ID.eq(guildId)
                        .and(MEMBERS_PUNISHMENTS.USER_ID.eq(userId))
                        .and(MEMBERS_PUNISHMENTS.ACTIVE.eq(true))
        );
    }

    public int countByUserIdAfter(long userId, long periodSeconds) {
        long now = Bot.unixNow();
        long startPoint = now - periodSeconds;

        return ctx.fetchCount(MEMBERS_PUNISHMENTS,
                MEMBERS_PUNISHMENTS.USER_ID.eq(userId)
                        .and(MEMBERS_PUNISHMENTS.ACTIVE.eq(true))
                        .and(MEMBERS_PUNISHMENTS.CREATED_AT.ge(startPoint))
        );
    }
}