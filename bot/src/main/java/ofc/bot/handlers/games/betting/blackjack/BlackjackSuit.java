package ofc.bot.handlers.games.betting.blackjack;

public enum BlackjackSuit {
    CLUBS("c", "C"),
    DIAMONDS("d", "D"),
    HEARTS("h", "H"),
    SPADES("s", "S");

    private final String emojiToken;
    private final String label;

    BlackjackSuit(String emojiToken, String label) {
        this.emojiToken = emojiToken;
        this.label = label;
    }

    public String emojiToken() {
        return emojiToken;
    }

    public String label() {
        return label;
    }
}
