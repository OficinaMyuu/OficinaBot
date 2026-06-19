package ofc.bot.handlers.games.betting.blackjack;

public enum BlackjackRank {
    ACE("a", "A", 11),
    TWO("2", "2", 2),
    THREE("3", "3", 3),
    FOUR("4", "4", 4),
    FIVE("5", "5", 5),
    SIX("6", "6", 6),
    SEVEN("7", "7", 7),
    EIGHT("8", "8", 8),
    NINE("9", "9", 9),
    TEN("10", "10", 10),
    JACK("j", "J", 10),
    QUEEN("q", "Q", 10),
    KING("k", "K", 10);

    private final String emojiToken;
    private final String label;
    private final int value;

    BlackjackRank(String emojiToken, String label, int value) {
        this.emojiToken = emojiToken;
        this.label = label;
        this.value = value;
    }

    public String emojiToken() {
        return emojiToken;
    }

    public String label() {
        return label;
    }

    public int value() {
        return value;
    }
}
