package ofc.bot.jobs.income;

import ofc.bot.domain.entity.VoiceChannelIncomeRule;
import ofc.bot.domain.entity.enums.VoiceChannelIncomePayoutType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VoiceIncomeRuleCacheTest {
    @Test
    void shouldUseDefaultsWhenRuleIsMissing() {
        VoiceIncomeRuleCache cache = new VoiceIncomeRuleCache();

        assertFalse(cache.hasRule(10L, VoiceChannelIncomePayoutType.MONEY));
        assertEquals(1.0D, cache.multiplierFor(10L, VoiceChannelIncomePayoutType.MONEY));
        assertFalse(cache.allowsMuted(10L, VoiceChannelIncomePayoutType.MONEY));
        assertFalse(cache.allowsSolo(10L, VoiceChannelIncomePayoutType.MONEY));
    }

    @Test
    void shouldResolveRulesByChannelAndPayoutType() {
        VoiceIncomeRuleCache cache = VoiceIncomeRuleCache.from(List.of(
                rule(10L, VoiceChannelIncomePayoutType.MONEY, 1.25D, true, true),
                rule(10L, VoiceChannelIncomePayoutType.LEVEL_EXPERIENCE, 1.50D, false, true)
        ));

        assertTrue(cache.hasRule(10L, VoiceChannelIncomePayoutType.MONEY));
        assertEquals(1.25D, cache.multiplierFor(10L, VoiceChannelIncomePayoutType.MONEY));
        assertTrue(cache.allowsMuted(10L, VoiceChannelIncomePayoutType.MONEY));
        assertTrue(cache.allowsSolo(10L, VoiceChannelIncomePayoutType.MONEY));

        assertEquals(1.50D, cache.multiplierFor(10L, VoiceChannelIncomePayoutType.LEVEL_EXPERIENCE));
        assertFalse(cache.allowsMuted(10L, VoiceChannelIncomePayoutType.LEVEL_EXPERIENCE));
        assertTrue(cache.allowsSolo(10L, VoiceChannelIncomePayoutType.LEVEL_EXPERIENCE));
    }

    @Test
    void shouldFallBackToDefaultMultiplierWhenRuleHasInvalidValue() {
        VoiceIncomeRuleCache cache = VoiceIncomeRuleCache.from(List.of(
                rule(10L, VoiceChannelIncomePayoutType.MONEY, -1.0D, true, true)
        ));

        assertEquals(1.0D, cache.multiplierFor(10L, VoiceChannelIncomePayoutType.MONEY));
    }

    @Test
    void shouldAllowMutedMembersOnlyWhenRuleAllowsIt() {
        VoiceIncomeRuleCache cache = VoiceIncomeRuleCache.from(List.of(
                rule(10L, VoiceChannelIncomePayoutType.MONEY, 1.25D, true, true)
        ));

        assertFalse(VoiceIncomeUtil.isEligibleVoiceState(
                false, true, false, 10L, new VoiceIncomeRuleCache(), VoiceChannelIncomePayoutType.MONEY
        ));
        assertTrue(VoiceIncomeUtil.isEligibleVoiceState(
                false, true, false, 10L, cache, VoiceChannelIncomePayoutType.MONEY
        ));
    }

    @Test
    void shouldAlwaysRejectBotsAndDeafenedMembers() {
        VoiceIncomeRuleCache cache = VoiceIncomeRuleCache.from(List.of(
                rule(10L, VoiceChannelIncomePayoutType.MONEY, 1.25D, true, true)
        ));

        assertFalse(VoiceIncomeUtil.isEligibleVoiceState(
                true, false, false, 10L, cache, VoiceChannelIncomePayoutType.MONEY
        ));
        assertFalse(VoiceIncomeUtil.isEligibleVoiceState(
                false, false, true, 10L, cache, VoiceChannelIncomePayoutType.MONEY
        ));
    }

    @Test
    void shouldScaleXpWithMultiplier() {
        assertEquals(50, VoiceXPHandler.calculateXp(40, 1.25D));
    }

    private VoiceChannelIncomeRule rule(
            long channelId,
            VoiceChannelIncomePayoutType payoutType,
            double multiplier,
            boolean allowMuted,
            boolean allowSolo
    ) {
        return new VoiceChannelIncomeRule(1L, channelId, payoutType, multiplier, allowMuted, allowSolo, 5L, 100L);
    }
}
