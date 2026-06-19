package ofc.bot.domain.entity;

import ofc.bot.domain.entity.enums.VoiceChannelIncomePayoutType;
import ofc.bot.domain.tables.VoiceChannelIncomeRulesTable;
import org.jetbrains.annotations.NotNull;

/**
 * Stores a per-channel override for one scheduled voice income payout type.
 */
public class VoiceChannelIncomeRule extends OficinaRecord<VoiceChannelIncomeRule> {
    private static final VoiceChannelIncomeRulesTable VOICE_CHANNEL_INCOME_RULES =
            VoiceChannelIncomeRulesTable.VOICE_CHANNEL_INCOME_RULES;

    /**
     * Creates an empty jOOQ record instance for fetch mapping.
     */
    public VoiceChannelIncomeRule() {
        super(VOICE_CHANNEL_INCOME_RULES);
    }

    /**
     * Creates a rule ready to be inserted or upserted.
     */
    public VoiceChannelIncomeRule(
            long guildId,
            long channelId,
            VoiceChannelIncomePayoutType payoutType,
            double multiplier,
            boolean allowMuted,
            boolean allowSolo,
            long createdBy,
            long createdAt
    ) {
        this();
        set(VOICE_CHANNEL_INCOME_RULES.GUILD_ID, guildId);
        set(VOICE_CHANNEL_INCOME_RULES.CHANNEL_ID, channelId);
        set(VOICE_CHANNEL_INCOME_RULES.PAYOUT_TYPE, payoutType.name());
        set(VOICE_CHANNEL_INCOME_RULES.MULTIPLIER, multiplier);
        set(VOICE_CHANNEL_INCOME_RULES.ALLOW_MUTED, allowMuted);
        set(VOICE_CHANNEL_INCOME_RULES.ALLOW_SOLO, allowSolo);
        set(VOICE_CHANNEL_INCOME_RULES.CREATED_BY, createdBy);
        set(VOICE_CHANNEL_INCOME_RULES.CREATED_AT, createdAt);
        set(VOICE_CHANNEL_INCOME_RULES.UPDATED_AT, createdAt);
    }

    /**
     * Returns the database primary key.
     */
    public int getId() {
        return get(VOICE_CHANNEL_INCOME_RULES.ID);
    }

    /**
     * Returns the Discord guild where the rule applies.
     */
    public long getGuildId() {
        return get(VOICE_CHANNEL_INCOME_RULES.GUILD_ID);
    }

    /**
     * Returns the Discord voice channel configured by the rule.
     */
    public long getChannelId() {
        return get(VOICE_CHANNEL_INCOME_RULES.CHANNEL_ID);
    }

    /**
     * Returns the payout type affected by this rule.
     */
    public VoiceChannelIncomePayoutType getPayoutType() {
        return VoiceChannelIncomePayoutType.valueOf(get(VOICE_CHANNEL_INCOME_RULES.PAYOUT_TYPE));
    }

    /**
     * Returns the payout multiplier, where 1.25 means 125% of the base value.
     */
    public double getMultiplier() {
        return get(VOICE_CHANNEL_INCOME_RULES.MULTIPLIER);
    }

    /**
     * Returns whether muted members can receive this payout in the channel.
     */
    public boolean allowsMuted() {
        return get(VOICE_CHANNEL_INCOME_RULES.ALLOW_MUTED);
    }

    /**
     * Returns whether a single undeafened human member is enough for this payout.
     */
    public boolean allowsSolo() {
        return get(VOICE_CHANNEL_INCOME_RULES.ALLOW_SOLO);
    }

    /**
     * Returns the Discord user that created the rule.
     */
    public long getCreatedBy() {
        return get(VOICE_CHANNEL_INCOME_RULES.CREATED_BY);
    }

    /**
     * Returns when this rule was created.
     */
    public long getTimeCreated() {
        return get(VOICE_CHANNEL_INCOME_RULES.CREATED_AT);
    }

    /**
     * Returns when this rule was last updated.
     */
    @Override
    public long getLastUpdated() {
        return get(VOICE_CHANNEL_INCOME_RULES.UPDATED_AT);
    }

    /**
     * Changes the payout multiplier.
     */
    public VoiceChannelIncomeRule setMultiplier(double multiplier) {
        set(VOICE_CHANNEL_INCOME_RULES.MULTIPLIER, multiplier);
        return this;
    }

    /**
     * Changes whether muted members can receive this payout.
     */
    public VoiceChannelIncomeRule setAllowMuted(boolean allowMuted) {
        set(VOICE_CHANNEL_INCOME_RULES.ALLOW_MUTED, allowMuted);
        return this;
    }

    /**
     * Changes whether one undeafened human member is enough for this payout.
     */
    public VoiceChannelIncomeRule setAllowSolo(boolean allowSolo) {
        set(VOICE_CHANNEL_INCOME_RULES.ALLOW_SOLO, allowSolo);
        return this;
    }

    /**
     * Updates the record timestamp.
     */
    @NotNull
    @Override
    public VoiceChannelIncomeRule setLastUpdated(long timestamp) {
        set(VOICE_CHANNEL_INCOME_RULES.UPDATED_AT, timestamp);
        return this;
    }
}
