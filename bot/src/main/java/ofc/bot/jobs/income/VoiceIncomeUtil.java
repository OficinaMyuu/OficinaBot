package ofc.bot.jobs.income;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import ofc.bot.domain.entity.enums.VoiceChannelIncomePayoutType;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared voice-channel eligibility rules for scheduled money and XP income.
 */
class VoiceIncomeUtil {
    private static final List<String> NON_ELIGIBLE_CATEGORIES = List.of(
            "587036164926734337",
            "691194660902928435",
            "664972695129030656"
    );

    /**
     * Returns members eligible under the default voice income rules.
     */
    static List<Member> getEligibleMembers(List<Guild> guilds) {
        return getEligibleMembers(guilds, new VoiceIncomeRuleCache(), VoiceChannelIncomePayoutType.MONEY);
    }

    /**
     * Returns members eligible for a payout type using the provided cached rules.
     */
    static List<Member> getEligibleMembers(
            List<Guild> guilds,
            VoiceIncomeRuleCache rules,
            VoiceChannelIncomePayoutType payoutType
    ) {
        List<Member> members = new ArrayList<>();
        for (Guild guild : guilds) {
            members.addAll(getEligibleMembers(guild, rules, payoutType));
        }
        return members;
    }

    /**
     * Returns whether a voice state can receive payout in the channel.
     */
    static boolean isEligibleVoiceState(
            boolean isBot,
            boolean isMuted,
            boolean isDeafened,
            long channelId,
            VoiceIncomeRuleCache rules,
            VoiceChannelIncomePayoutType payoutType
    ) {
        if (isBot || isDeafened) return false;
        return !isMuted || rules.allowsMuted(channelId, payoutType);
    }

    /**
     * Scales a positive payout and preserves at least one unit.
     */
    static int scalePayout(int amount, double multiplier) {
        if (amount <= 0) return 0;
        if (!Double.isFinite(multiplier) || multiplier <= 0) return amount;

        return Math.max(1, (int) Math.round(amount * multiplier));
    }

    /**
     * Returns members eligible for one guild and payout type.
     */
    private static List<Member> getEligibleMembers(
            Guild guild,
            VoiceIncomeRuleCache rules,
            VoiceChannelIncomePayoutType payoutType
    ) {
        List<VoiceChannel> voiceChannels = getEligibleVoiceChannels(guild, rules, payoutType);
        return voiceChannels.stream()
                .filter(vc -> hasEnoughMembers(vc, rules, payoutType))
                .flatMap(vc -> vc.getMembers().stream())
                .filter(m -> isEligibleMember(m, rules, payoutType))
                .toList();
    }

    /**
     * Returns whether a channel satisfies the minimum human presence rule.
     */
    private static boolean hasEnoughMembers(
            VoiceChannel vc,
            VoiceIncomeRuleCache rules,
            VoiceChannelIncomePayoutType payoutType
    ) {
        long channelId = vc.getIdLong();
        if (rules.allowsSolo(channelId, payoutType)) return true;

        List<Member> undeafenedMembers = vc.getMembers()
                .stream()
                .filter(m -> !m.getUser().isBot())
                .filter(m -> m.getVoiceState() != null && !m.getVoiceState().isDeafened())
                .toList();

        return undeafenedMembers.size() >= 2;
    }

    /**
     * Returns channels eligible either by default category or explicit rule.
     */
    private static List<VoiceChannel> getEligibleVoiceChannels(
            Guild guild,
            VoiceIncomeRuleCache rules,
            VoiceChannelIncomePayoutType payoutType
    ) {
        return guild.getVoiceChannels()
                .stream()
                .filter(vc -> isEligible(vc, rules, payoutType))
                .toList();
    }

    /**
     * Returns whether a channel can participate in this payout type.
     */
    private static boolean isEligible(VoiceChannel vc, VoiceIncomeRuleCache rules, VoiceChannelIncomePayoutType payoutType) {
        Category parentCategory = vc.getParentCategory();

        return rules.hasRule(vc.getIdLong(), payoutType)
                || (parentCategory != null && !NON_ELIGIBLE_CATEGORIES.contains(parentCategory.getId()));
    }

    /**
     * Returns whether the member can receive the payout according to voice state.
     */
    private static boolean isEligibleMember(
            Member member,
            VoiceIncomeRuleCache rules,
            VoiceChannelIncomePayoutType payoutType
    ) {
        var voiceState = member.getVoiceState();
        if (voiceState == null || voiceState.getChannel() == null) return false;

        return isEligibleVoiceState(
                member.getUser().isBot(),
                voiceState.isMuted(),
                voiceState.isDeafened(),
                voiceState.getChannel().getIdLong(),
                rules,
                payoutType
        );
    }
}
