package ofc.bot.domain.entity;

import ofc.bot.domain.tables.GiveawayEntriesTable;
import org.jetbrains.annotations.NotNull;

public class GiveawayEntry extends OficinaRecord<GiveawayEntry> {
    private static final GiveawayEntriesTable GIVEAWAY_ENTRIES = GiveawayEntriesTable.GIVEAWAY_ENTRIES;

    public GiveawayEntry() {
        super(GIVEAWAY_ENTRIES);
    }

    public GiveawayEntry(@NotNull String giveawayId, long userId, long createdAt) {
        this();
        setGiveawayId(giveawayId);
        setUserId(userId);
        setTimeCreated(createdAt);
    }

    public String getGiveawayId() {
        return get(GIVEAWAY_ENTRIES.GIVEAWAY_ID);
    }

    public long getUserId() {
        return get(GIVEAWAY_ENTRIES.USER_ID);
    }

    public long getTimeCreated() {
        return get(GIVEAWAY_ENTRIES.CREATED_AT);
    }

    public GiveawayEntry setGiveawayId(@NotNull String giveawayId) {
        set(GIVEAWAY_ENTRIES.GIVEAWAY_ID, giveawayId);
        return this;
    }

    public GiveawayEntry setUserId(long userId) {
        set(GIVEAWAY_ENTRIES.USER_ID, userId);
        return this;
    }

    public GiveawayEntry setTimeCreated(long createdAt) {
        set(GIVEAWAY_ENTRIES.CREATED_AT, createdAt);
        return this;
    }
}
