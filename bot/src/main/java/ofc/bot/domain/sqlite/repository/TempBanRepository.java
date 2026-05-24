package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.TempBan;
import ofc.bot.domain.tables.TempBansTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

/**
 * Repository for {@link TempBan} entity.
 */
public class TempBanRepository extends Repository<TempBan> {
    private static final TempBansTable TEMP_BANS = TempBansTable.TEMP_BANS;

    public TempBanRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<TempBan> getTable() {
        return TEMP_BANS;
    }

    public void deleteByUserAndGuildId(long userId, long guildId) {
        ctx.deleteFrom(TEMP_BANS)
                .where(TEMP_BANS.GUILD_ID.eq(guildId))
                .and(TEMP_BANS.USER_ID.eq(userId))
                .execute();
    }

    public void deleteIn(List<TempBan> bans) {
        List<Integer> ids = bans.stream().map(TempBan::getId).toList();
        ctx.deleteFrom(TEMP_BANS)
                .where(TEMP_BANS.ID.in(ids))
                .execute();
    }

    public List<TempBan> findFromBefore(long timestamp) {
        return ctx.selectFrom(TEMP_BANS)
                .where(TEMP_BANS.EXPIRES_AT.le(timestamp))
                .fetch();
    }
}