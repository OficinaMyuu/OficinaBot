package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.GiveawayEntry;
import ofc.bot.domain.tables.GiveawayEntriesTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

public class GiveawayEntryRepository extends Repository<GiveawayEntry> {
    private static final GiveawayEntriesTable GIVEAWAY_ENTRIES = GiveawayEntriesTable.GIVEAWAY_ENTRIES;

    public GiveawayEntryRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<GiveawayEntry> getTable() {
        return GIVEAWAY_ENTRIES;
    }

    public boolean addEntry(@NotNull String giveawayId, long userId, long createdAt) {
        return ctx.insertInto(GIVEAWAY_ENTRIES)
                .set(new GiveawayEntry(giveawayId, userId, createdAt))
                .onConflictDoNothing()
                .execute() == 1;
    }

    public boolean removeEntry(@NotNull String giveawayId, long userId) {
        return ctx.deleteFrom(GIVEAWAY_ENTRIES)
                .where(GIVEAWAY_ENTRIES.GIVEAWAY_ID.eq(giveawayId))
                .and(GIVEAWAY_ENTRIES.USER_ID.eq(userId))
                .execute() == 1;
    }

    public int removeUserFromVoiceLockedGiveaways(long userId, long voiceChannelId) {
        return ctx.deleteFrom(GIVEAWAY_ENTRIES)
                .where(GIVEAWAY_ENTRIES.USER_ID.eq(userId))
                .and(GIVEAWAY_ENTRIES.GIVEAWAY_ID.in(
                        ctx.select(ofc.bot.domain.tables.GiveawaysTable.GIVEAWAYS.GIVEAWAY_ID)
                                .from(ofc.bot.domain.tables.GiveawaysTable.GIVEAWAYS)
                                .where(ofc.bot.domain.tables.GiveawaysTable.GIVEAWAYS.REQUIRED_VOICE_CHANNEL_ID.eq(voiceChannelId))
                                .and(ofc.bot.domain.tables.GiveawaysTable.GIVEAWAYS.STATUS.eq(
                                        ofc.bot.domain.entity.enums.GiveawayStatus.ACTIVE.name()
                                ))
                ))
                .execute();
    }

    public boolean hasEntry(@NotNull String giveawayId, long userId) {
        return ctx.fetchExists(
                ctx.selectOne()
                        .from(GIVEAWAY_ENTRIES)
                        .where(GIVEAWAY_ENTRIES.GIVEAWAY_ID.eq(giveawayId))
                        .and(GIVEAWAY_ENTRIES.USER_ID.eq(userId))
        );
    }

    public int countByGiveaway(@NotNull String giveawayId) {
        return ctx.fetchCount(
                ctx.selectFrom(GIVEAWAY_ENTRIES)
                        .where(GIVEAWAY_ENTRIES.GIVEAWAY_ID.eq(giveawayId))
        );
    }

    public List<GiveawayEntry> findByGiveaway(@NotNull String giveawayId) {
        return ctx.selectFrom(GIVEAWAY_ENTRIES)
                .where(GIVEAWAY_ENTRIES.GIVEAWAY_ID.eq(giveawayId))
                .fetch();
    }

    public List<String> findActiveGiveawaysForVoiceChannel(long voiceChannelId) {
        var giveaways = ofc.bot.domain.tables.GiveawaysTable.GIVEAWAYS;

        return ctx.select(giveaways.GIVEAWAY_ID)
                .from(giveaways)
                .where(giveaways.REQUIRED_VOICE_CHANNEL_ID.eq(voiceChannelId))
                .and(giveaways.STATUS.eq(ofc.bot.domain.entity.enums.GiveawayStatus.ACTIVE.name()))
                .fetch(giveaways.GIVEAWAY_ID);
    }
}
