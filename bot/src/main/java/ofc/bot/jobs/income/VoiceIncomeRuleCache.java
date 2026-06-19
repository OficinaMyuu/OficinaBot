package ofc.bot.jobs.income;

import ofc.bot.domain.entity.VoiceChannelIncomeRule;
import ofc.bot.domain.entity.enums.VoiceChannelIncomePayoutType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable lookup of voice income rules loaded once for a scheduled payout run.
 */
class VoiceIncomeRuleCache {
    private static final double DEFAULT_MULTIPLIER = 1.0D;
    private final Map<RuleKey, VoiceChannelIncomeRule> rules;

    /**
     * Creates an empty cache that keeps default voice income behavior.
     */
    VoiceIncomeRuleCache() {
        this(Map.of());
    }

    /**
     * Creates a cache from a prepared rule map.
     */
    private VoiceIncomeRuleCache(Map<RuleKey, VoiceChannelIncomeRule> rules) {
        this.rules = Map.copyOf(rules);
    }

    /**
     * Builds a cache from database rows, keyed by channel and payout type.
     */
    static VoiceIncomeRuleCache from(@NotNull List<VoiceChannelIncomeRule> rules) {
        Map<RuleKey, VoiceChannelIncomeRule> mappedRules = new HashMap<>();
        for (VoiceChannelIncomeRule rule : rules) {
            mappedRules.put(new RuleKey(rule.getChannelId(), rule.getPayoutType()), rule);
        }
        return new VoiceIncomeRuleCache(mappedRules);
    }

    /**
     * Returns whether the channel has an explicit rule for the payout type.
     */
    boolean hasRule(long channelId, @NotNull VoiceChannelIncomePayoutType payoutType) {
        return rules.containsKey(new RuleKey(channelId, payoutType));
    }

    /**
     * Returns the configured multiplier, or 1.0 when no valid rule exists.
     */
    double multiplierFor(long channelId, @NotNull VoiceChannelIncomePayoutType payoutType) {
        VoiceChannelIncomeRule rule = getRule(channelId, payoutType);
        if (rule == null) return DEFAULT_MULTIPLIER;

        double multiplier = rule.getMultiplier();
        return Double.isFinite(multiplier) && multiplier > 0 ? multiplier : DEFAULT_MULTIPLIER;
    }

    /**
     * Returns whether muted members are eligible for this payout in the channel.
     */
    boolean allowsMuted(long channelId, @NotNull VoiceChannelIncomePayoutType payoutType) {
        VoiceChannelIncomeRule rule = getRule(channelId, payoutType);
        return rule != null && rule.allowsMuted();
    }

    /**
     * Returns whether solo human members are eligible for this payout in the channel.
     */
    boolean allowsSolo(long channelId, @NotNull VoiceChannelIncomePayoutType payoutType) {
        VoiceChannelIncomeRule rule = getRule(channelId, payoutType);
        return rule != null && rule.allowsSolo();
    }

    /**
     * Finds a rule by channel and payout type.
     */
    private VoiceChannelIncomeRule getRule(long channelId, VoiceChannelIncomePayoutType payoutType) {
        return rules.get(new RuleKey(channelId, payoutType));
    }

    /**
     * Key used to separate MONEY and LEVEL_EXPERIENCE rules for the same channel.
     */
    private record RuleKey(long channelId, VoiceChannelIncomePayoutType payoutType) {}
}
