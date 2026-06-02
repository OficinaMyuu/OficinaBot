package ofc.bot.handlers.games.betting.roulette;

public record RouletteEntry(long userId, RouletteBet bet, int amount, long createdAt) {
    public int maxPayout() {
        return bet.payoutFor(amount);
    }
}
