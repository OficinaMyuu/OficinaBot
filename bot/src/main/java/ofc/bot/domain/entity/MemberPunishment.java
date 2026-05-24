package ofc.bot.domain.entity;

import net.dv8tion.jda.api.entities.Member;
import ofc.bot.domain.tables.MembersPunishmentsTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

public class MemberPunishment extends OficinaRecord<MemberPunishment> {
    private static final MembersPunishmentsTable MEMBERS_PUNISHMENTS = MembersPunishmentsTable.MEMBERS_PUNISHMENTS;

    public MemberPunishment() {
        super(MEMBERS_PUNISHMENTS);
    }

    public MemberPunishment(
            long guildId, long userId, long moderatorId, @NotNull String reason, long createdAt, long updatedAt
    ) {
        this();
        set(MEMBERS_PUNISHMENTS.GUILD_ID, guildId);
        set(MEMBERS_PUNISHMENTS.USER_ID, userId);
        set(MEMBERS_PUNISHMENTS.MODERATOR_ID, moderatorId);
        set(MEMBERS_PUNISHMENTS.REASON, reason);
        set(MEMBERS_PUNISHMENTS.ACTIVE, true);
        set(MEMBERS_PUNISHMENTS.CREATED_AT, createdAt);
        set(MEMBERS_PUNISHMENTS.UPDATED_AT, updatedAt);
    }

    public static MemberPunishment fromMember(@NotNull Member member, long moderatorId, @NotNull String reason) {
        long guildId = member.getGuild().getIdLong();
        long timestamp = Bot.unixNow();
        return new MemberPunishment(guildId, member.getIdLong(), moderatorId, reason, timestamp, timestamp);
    }

    public int getId() {
        return get(MEMBERS_PUNISHMENTS.ID);
    }

    public long getGuildId() {
        return get(MEMBERS_PUNISHMENTS.GUILD_ID);
    }

    public long getUserId() {
        return get(MEMBERS_PUNISHMENTS.USER_ID);
    }

    public long getModeratorId() {
        return get(MEMBERS_PUNISHMENTS.MODERATOR_ID);
    }

    public String getReason() {
        return get(MEMBERS_PUNISHMENTS.REASON);
    }

    public boolean isActive() {
        return get(MEMBERS_PUNISHMENTS.ACTIVE);
    }

    public long getDeletionAuthorId() {
        return get(MEMBERS_PUNISHMENTS.DELETION_AUTHOR_ID);
    }

    public String getDeletionAuthorMention() {
        Long id = get(MEMBERS_PUNISHMENTS.DELETION_AUTHOR_ID);
        return id == null ? null : "<@" + id + '>';
    }

    public long getTimeCreated() {
        return get(MEMBERS_PUNISHMENTS.CREATED_AT);
    }

    public long getLastUpdated() {
        return get(MEMBERS_PUNISHMENTS.UPDATED_AT);
    }

    public MemberPunishment setActive(boolean active) {
        set(MEMBERS_PUNISHMENTS.ACTIVE, active);
        return this;
    }

    public MemberPunishment setDeletionAuthorId(long deletionAuthorId) {
        set(MEMBERS_PUNISHMENTS.DELETION_AUTHOR_ID, deletionAuthorId);
        return this;
    }

    @NotNull
    public MemberPunishment setLastUpdated(long updatedAt) {
        set(MEMBERS_PUNISHMENTS.UPDATED_AT, updatedAt);
        return this;
    }
}