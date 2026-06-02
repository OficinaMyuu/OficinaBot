package ofc.bot.handlers.games.betting.roulette;

public record RouletteResolvedEntry(RouletteEntry entry, boolean won, int payout) {
    public static RouletteResolvedEntry resolve(RouletteEntry entry, RouletteSpin spin) {
        boolean won = entry.bet().wins(spin);
        return new RouletteResolvedEntry(entry, won, won ? entry.maxPayout() : 0);
    }
}
