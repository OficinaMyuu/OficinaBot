package ofc.bot.domain.entity;

import ofc.bot.domain.tables.MembersEmojisTable;
import org.jetbrains.annotations.NotNull;

public class MemberEmoji extends OficinaRecord<MemberEmoji> {
    private static final MembersEmojisTable MEMBERS_EMOJIS = MembersEmojisTable.MEMBERS_EMOJIS;

    public MemberEmoji() {
        super(MEMBERS_EMOJIS);
    }

    public long getUserId() {
        return get(MEMBERS_EMOJIS.USER_ID);
    }

    public String getEmoji() {
        return get(MEMBERS_EMOJIS.EMOJI);
    }

    public long getTimeCreated() {
        return get(MEMBERS_EMOJIS.CREATED_AT);
    }

    public long getLastUpdated() {
        return get(MEMBERS_EMOJIS.UPDATED_AT);
    }

    public MemberEmoji setUserId(long userId) {
        set(MEMBERS_EMOJIS.USER_ID, userId);
        return this;
    }

    public MemberEmoji setEmoji(String emoji) {
        set(MEMBERS_EMOJIS.EMOJI, emoji);
        return this;
    }

    @NotNull
    public MemberEmoji setLastUpdated(long updatedAt) {
        set(MEMBERS_EMOJIS.UPDATED_AT, updatedAt);
        return this;
    }
}