package ofc.bot.handlers.games.betting.blackjack;

public record BlackjackResolvedHand(BlackjackHand hand, BlackjackOutcome outcome) {
    public int credit() {
        return switch (outcome) {
            case WIN -> hand.stake() * 2;
            case PUSH -> hand.stake();
            case LOSS -> 0;
        };
    }

    public int net() {
        return credit() - hand.stake();
    }
}
