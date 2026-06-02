package ofc.bot.handlers.games.betting.roulette;

import java.util.*;
import java.util.stream.Collectors;

public record RouletteResult(RouletteSpin spin, List<RouletteResolvedEntry> entries) {
    public RouletteResult {
        entries = List.copyOf(entries);
    }

    public static RouletteResult resolve(RouletteSpin spin, List<RouletteEntry> entries) {
        List<RouletteResolvedEntry> resolved = entries.stream()
                .map(entry -> RouletteResolvedEntry.resolve(entry, spin))
                .toList();

        return new RouletteResult(spin, resolved);
    }

    public Map<Long, Integer> payoutsByUser() {
        return entries.stream()
                .filter(RouletteResolvedEntry::won)
                .collect(Collectors.toMap(
                        resolved -> resolved.entry().userId(),
                        RouletteResolvedEntry::payout,
                        Integer::sum,
                        LinkedHashMap::new
                ));
    }

    public Set<Long> winners() {
        return entries.stream()
                .filter(RouletteResolvedEntry::won)
                .map(resolved -> resolved.entry().userId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
