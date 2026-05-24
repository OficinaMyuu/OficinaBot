package ofc.bot.jobs.income;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

class VoiceIncomeUtil {
    private static final Predicate<Member> CONDITION = (m) -> m.getVoiceState() != null
            && !m.getVoiceState().isMuted()
            && !m.getVoiceState().isDeafened();

    private static final List<String> NON_ELIGIBLE_CATEGORIES = List.of(
            "587036164926734337",
            "691194660902928435",
            "664972695129030656"
    );

    static List<Member> getEligibleMembers(List<Guild> guilds) {
        List<Member> members = new ArrayList<>();
        for (Guild guild : guilds) {
            members.addAll(getEligibleMembers(guild));
        }
        return members;
    }

    private static List<Member> getEligibleMembers(Guild guild) {
        List<VoiceChannel> voiceChannels = getEligibleVoiceChannels(guild);
        return voiceChannels.stream()
                .filter(VoiceIncomeUtil::hasEnoughMembers)
                .flatMap(vc -> vc.getMembers().stream())
                .filter(CONDITION)
                .filter(m -> !m.getUser().isBot())
                .toList();
    }

    private static boolean hasEnoughMembers(VoiceChannel vc) {
        List<Member> undeafenedMembers = vc.getMembers()
                .stream()
                .filter(m -> !m.getUser().isBot())
                .filter(m -> m.getVoiceState() != null && !m.getVoiceState().isDeafened())
                .toList();

        return undeafenedMembers.size() >= 2;
    }

    private static List<VoiceChannel> getEligibleVoiceChannels(Guild guild) {
        return guild.getVoiceChannels()
                .stream()
                .filter(VoiceIncomeUtil::isEligible)
                .toList();
    }

    private static boolean isEligible(VoiceChannel vc) {
        Category parentCategory = vc.getParentCategory();

        return parentCategory != null && !NON_ELIGIBLE_CATEGORIES.contains(parentCategory.getId());
    }
}