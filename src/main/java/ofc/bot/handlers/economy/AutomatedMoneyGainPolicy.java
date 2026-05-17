package ofc.bot.handlers.economy;

import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import ofc.bot.domain.entity.enums.PolicyType;
import ofc.bot.handlers.cache.PolicyService;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

public class AutomatedMoneyGainPolicy {
    private final Supplier<Set<Long>> blockedResources;

    public AutomatedMoneyGainPolicy() {
        this(() -> PolicyService.getService().get(PolicyType.BLOCK_MONEY_GAINS, Long::parseLong));
    }

    AutomatedMoneyGainPolicy(@NotNull Supplier<Set<Long>> blockedResources) {
        this.blockedResources = blockedResources;
    }

    public boolean isBlocked(@NotNull Member member, long channelId) {
        return isBlocked(
                member.getIdLong(),
                member.getRoles().stream().map(ISnowflake::getIdLong).toList(),
                channelId
        );
    }

    boolean isBlocked(long userId, @NotNull Collection<Long> roleIds, long channelId) {
        Set<Long> blocked = blockedResources.get();
        return blocked.contains(userId)
                || blocked.contains(channelId)
                || roleIds.stream().anyMatch(blocked::contains);
    }
}
