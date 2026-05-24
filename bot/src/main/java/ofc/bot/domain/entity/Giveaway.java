package ofc.bot.domain.entity;

import ofc.bot.domain.entity.enums.GiveawayPrizeType;
import ofc.bot.domain.entity.enums.GiveawayStatus;
import ofc.bot.domain.tables.GiveawaysTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Giveaway extends OficinaRecord<Giveaway> {
    private static final GiveawaysTable GIVEAWAYS = GiveawaysTable.GIVEAWAYS;

    public Giveaway() {
        super(GIVEAWAYS);
    }

    public Giveaway(
            @NotNull String giveawayId,
            long guildId,
            long channelId,
            long messageId,
            long hostId,
            @NotNull GiveawayStatus status,
            @NotNull GiveawayPrizeType prizeType,
            @NotNull String title,
            @Nullable String description,
            int winnerCount,
            long endsAt,
            @Nullable Long endedAt,
            @Nullable Long requiredVoiceChannelId,
            @Nullable Long moneyAmount,
            @Nullable Long colorRoleDurationSeconds,
            long createdAt,
            long updatedAt
    ) {
        this();
        setGiveawayId(giveawayId);
        setGuildId(guildId);
        setChannelId(channelId);
        setMessageId(messageId);
        setHostId(hostId);
        setStatus(status);
        setPrizeType(prizeType);
        setTitle(title);
        setDescription(description);
        setWinnerCount(winnerCount);
        setEndsAt(endsAt);
        setEndedAt(endedAt);
        setRequiredVoiceChannelId(requiredVoiceChannelId);
        setMoneyAmount(moneyAmount);
        setColorRoleDurationSeconds(colorRoleDurationSeconds);
        setTimeCreated(createdAt);
        setLastUpdated(updatedAt);
    }

    public String getGiveawayId() {
        return get(GIVEAWAYS.GIVEAWAY_ID);
    }

    public long getGuildId() {
        return get(GIVEAWAYS.GUILD_ID);
    }

    public long getChannelId() {
        return get(GIVEAWAYS.CHANNEL_ID);
    }

    public long getMessageId() {
        return get(GIVEAWAYS.MESSAGE_ID);
    }

    public long getHostId() {
        return get(GIVEAWAYS.HOST_ID);
    }

    public GiveawayStatus getStatus() {
        return GiveawayStatus.valueOf(get(GIVEAWAYS.STATUS));
    }

    public GiveawayPrizeType getPrizeType() {
        return GiveawayPrizeType.valueOf(get(GIVEAWAYS.PRIZE_TYPE));
    }

    public String getTitle() {
        return get(GIVEAWAYS.TITLE);
    }

    public String getDescription() {
        return get(GIVEAWAYS.DESCRIPTION);
    }

    public int getWinnerCount() {
        return get(GIVEAWAYS.WINNER_COUNT);
    }

    public long getEndsAt() {
        return get(GIVEAWAYS.ENDS_AT);
    }

    public Long getEndedAt() {
        return get(GIVEAWAYS.ENDED_AT);
    }

    public Long getRequiredVoiceChannelId() {
        return get(GIVEAWAYS.REQUIRED_VOICE_CHANNEL_ID);
    }

    public Long getMoneyAmount() {
        return get(GIVEAWAYS.MONEY_AMOUNT);
    }

    public Long getColorRoleDurationSeconds() {
        return get(GIVEAWAYS.COLOR_ROLE_DURATION_SECONDS);
    }

    public long getTimeCreated() {
        return get(GIVEAWAYS.CREATED_AT);
    }

    @Override
    public long getLastUpdated() {
        return get(GIVEAWAYS.UPDATED_AT);
    }

    public boolean isActive() {
        return getStatus() == GiveawayStatus.ACTIVE;
    }

    public Giveaway setGiveawayId(@NotNull String giveawayId) {
        set(GIVEAWAYS.GIVEAWAY_ID, giveawayId);
        return this;
    }

    public Giveaway setGuildId(long guildId) {
        set(GIVEAWAYS.GUILD_ID, guildId);
        return this;
    }

    public Giveaway setChannelId(long channelId) {
        set(GIVEAWAYS.CHANNEL_ID, channelId);
        return this;
    }

    public Giveaway setMessageId(long messageId) {
        set(GIVEAWAYS.MESSAGE_ID, messageId);
        return this;
    }

    public Giveaway setHostId(long hostId) {
        set(GIVEAWAYS.HOST_ID, hostId);
        return this;
    }

    public Giveaway setStatus(@NotNull GiveawayStatus status) {
        set(GIVEAWAYS.STATUS, status.name());
        return this;
    }

    public Giveaway setPrizeType(@NotNull GiveawayPrizeType prizeType) {
        set(GIVEAWAYS.PRIZE_TYPE, prizeType.name());
        return this;
    }

    public Giveaway setTitle(@NotNull String title) {
        set(GIVEAWAYS.TITLE, title);
        return this;
    }

    public Giveaway setDescription(@Nullable String description) {
        set(GIVEAWAYS.DESCRIPTION, description);
        return this;
    }

    public Giveaway setWinnerCount(int winnerCount) {
        set(GIVEAWAYS.WINNER_COUNT, winnerCount);
        return this;
    }

    public Giveaway setEndsAt(long endsAt) {
        set(GIVEAWAYS.ENDS_AT, endsAt);
        return this;
    }

    public Giveaway setEndedAt(@Nullable Long endedAt) {
        set(GIVEAWAYS.ENDED_AT, endedAt);
        return this;
    }

    public Giveaway setRequiredVoiceChannelId(@Nullable Long requiredVoiceChannelId) {
        set(GIVEAWAYS.REQUIRED_VOICE_CHANNEL_ID, requiredVoiceChannelId);
        return this;
    }

    public Giveaway setMoneyAmount(@Nullable Long moneyAmount) {
        set(GIVEAWAYS.MONEY_AMOUNT, moneyAmount);
        return this;
    }

    public Giveaway setColorRoleDurationSeconds(@Nullable Long durationSeconds) {
        set(GIVEAWAYS.COLOR_ROLE_DURATION_SECONDS, durationSeconds);
        return this;
    }

    public Giveaway setTimeCreated(long createdAt) {
        set(GIVEAWAYS.CREATED_AT, createdAt);
        return this;
    }

    @NotNull
    @Override
    public Giveaway setLastUpdated(long updatedAt) {
        set(GIVEAWAYS.UPDATED_AT, updatedAt);
        return this;
    }
}
