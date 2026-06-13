package ofc.bot.handlers.games.betting.roulette;

public sealed interface RoulettePlacementResult {
    record Accepted(RouletteEntry entry, boolean firstBetInGame, boolean replacedExistingBet) implements RoulettePlacementResult {}

    record Rejected(Reason reason) implements RoulettePlacementResult {}

    enum Reason {
        GAME_CLOSED,
        ACTIVE_OTHER_GAME,
        INSUFFICIENT_BALANCE,
        INVALID_AMOUNT,
        PAYOUT_LIMIT
    }
}
