package ofc.bot.handlers.accumulator;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class AccumulatorMemberResolver {
    static final int MAX_GATEWAY_LOOKUP_IDS = 100;
    private static final int LOOKUP_TIMEOUT_SECONDS = 30;

    private final MemberIdFetcher fetcher;

    public AccumulatorMemberResolver() {
        this(AccumulatorMemberResolver::fetchFromDiscord);
    }

    AccumulatorMemberResolver(MemberIdFetcher fetcher) {
        this.fetcher = fetcher;
    }

    public Set<Long> findExistingMemberIds(Guild guild, Collection<Long> userIds) {
        Set<Long> distinctIds = userIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinctIds.isEmpty()) {
            return Set.of();
        }

        Set<Long> foundIds = new HashSet<>();
        List<Long> batch = new ArrayList<>(MAX_GATEWAY_LOOKUP_IDS);
        for (long userId : distinctIds) {
            batch.add(userId);
            if (batch.size() == MAX_GATEWAY_LOOKUP_IDS) {
                foundIds.addAll(fetcher.fetch(guild, List.copyOf(batch)));
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            foundIds.addAll(fetcher.fetch(guild, List.copyOf(batch)));
        }
        return foundIds;
    }

    private static Set<Long> fetchFromDiscord(Guild guild, List<Long> userIds) {
        return guild.retrieveMembersByIds(false, userIds)
                .setTimeout(LOOKUP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .get()
                .stream()
                .map(Member::getIdLong)
                .collect(Collectors.toSet());
    }

    @FunctionalInterface
    interface MemberIdFetcher {
        Set<Long> fetch(Guild guild, List<Long> userIds);
    }
}
