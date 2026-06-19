package ofc.bot.handlers.games.betting.blackjack;

public record BlackjackHandValue(int total, boolean soft) {
    public boolean busted() {
        return total > 21;
    }

    public String display() {
        return soft ? "Soft " + total : String.valueOf(total);
    }
}
