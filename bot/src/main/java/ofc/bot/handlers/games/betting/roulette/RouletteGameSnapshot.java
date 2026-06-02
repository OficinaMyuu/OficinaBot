package ofc.bot.handlers.games.betting.roulette;

import ofc.bot.handlers.games.GameStatus;

import java.util.List;

public record RouletteGameSnapshot(
        long id,
        long channelId,
        GameStatus status,
        long endsAt,
        List<RouletteEntry> entries
) {
    public RouletteGameSnapshot {
        entries = List.copyOf(entries);
    }
}
