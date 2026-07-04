package ofc.bot.domain.database.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.Giveaway;
import ofc.bot.domain.entity.enums.GiveawayStatus;
import ofc.bot.domain.tables.GiveawaysTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

public class GiveawayRepository extends Repository<Giveaway> {
    private static final GiveawaysTable GIVEAWAYS = GiveawaysTable.GIVEAWAYS;

    public GiveawayRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<Giveaway> getTable() {
        return GIVEAWAYS;
    }

    public Giveaway findById(@NotNull String giveawayId) {
        return ctx.selectFrom(GIVEAWAYS)
                .where(GIVEAWAYS.GIVEAWAY_ID.eq(giveawayId))
                .fetchOne();
    }

    public Giveaway findByMessageId(long messageId) {
        return ctx.selectFrom(GIVEAWAYS)
                .where(GIVEAWAYS.MESSAGE_ID.eq(messageId))
                .fetchOne();
    }

    public Giveaway findByIdOrMessageId(@NotNull String identifier) {
        Giveaway byId = findById(identifier);
        if (byId != null) {
            return byId;
        }

        try {
            return findByMessageId(Long.parseLong(identifier));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public List<Giveaway> findDueActive(long now) {
        return ctx.selectFrom(GIVEAWAYS)
                .where(GIVEAWAYS.STATUS.eq(GiveawayStatus.ACTIVE.name()))
                .and(GIVEAWAYS.ENDS_AT.le(now))
                .fetch();
    }

    public boolean markEnded(@NotNull String giveawayId, long endedAt) {
        return ctx.update(GIVEAWAYS)
                .set(GIVEAWAYS.STATUS, GiveawayStatus.ENDED.name())
                .set(GIVEAWAYS.ENDED_AT, endedAt)
                .set(GIVEAWAYS.UPDATED_AT, endedAt)
                .where(GIVEAWAYS.GIVEAWAY_ID.eq(giveawayId))
                .and(GIVEAWAYS.STATUS.eq(GiveawayStatus.ACTIVE.name()))
                .execute() == 1;
    }
}
