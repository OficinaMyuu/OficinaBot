package ofc.bot.handlers.games.betting.blackjack;

public enum BlackjackCard {
    ACE_OF_CLUBS(BlackjackRank.ACE, BlackjackSuit.CLUBS, 0L),
    TWO_OF_CLUBS(BlackjackRank.TWO, BlackjackSuit.CLUBS, 0L),
    THREE_OF_CLUBS(BlackjackRank.THREE, BlackjackSuit.CLUBS, 0L),
    FOUR_OF_CLUBS(BlackjackRank.FOUR, BlackjackSuit.CLUBS, 0L),
    FIVE_OF_CLUBS(BlackjackRank.FIVE, BlackjackSuit.CLUBS, 0L),
    SIX_OF_CLUBS(BlackjackRank.SIX, BlackjackSuit.CLUBS, 0L),
    SEVEN_OF_CLUBS(BlackjackRank.SEVEN, BlackjackSuit.CLUBS, 0L),
    EIGHT_OF_CLUBS(BlackjackRank.EIGHT, BlackjackSuit.CLUBS, 0L),
    NINE_OF_CLUBS(BlackjackRank.NINE, BlackjackSuit.CLUBS, 0L),
    TEN_OF_CLUBS(BlackjackRank.TEN, BlackjackSuit.CLUBS, 0L),
    JACK_OF_CLUBS(BlackjackRank.JACK, BlackjackSuit.CLUBS, 0L),
    QUEEN_OF_CLUBS(BlackjackRank.QUEEN, BlackjackSuit.CLUBS, 0L),
    KING_OF_CLUBS(BlackjackRank.KING, BlackjackSuit.CLUBS, 0L),

    ACE_OF_DIAMONDS(BlackjackRank.ACE, BlackjackSuit.DIAMONDS, 0L),
    TWO_OF_DIAMONDS(BlackjackRank.TWO, BlackjackSuit.DIAMONDS, 0L),
    THREE_OF_DIAMONDS(BlackjackRank.THREE, BlackjackSuit.DIAMONDS, 0L),
    FOUR_OF_DIAMONDS(BlackjackRank.FOUR, BlackjackSuit.DIAMONDS, 0L),
    FIVE_OF_DIAMONDS(BlackjackRank.FIVE, BlackjackSuit.DIAMONDS, 0L),
    SIX_OF_DIAMONDS(BlackjackRank.SIX, BlackjackSuit.DIAMONDS, 0L),
    SEVEN_OF_DIAMONDS(BlackjackRank.SEVEN, BlackjackSuit.DIAMONDS, 0L),
    EIGHT_OF_DIAMONDS(BlackjackRank.EIGHT, BlackjackSuit.DIAMONDS, 0L),
    NINE_OF_DIAMONDS(BlackjackRank.NINE, BlackjackSuit.DIAMONDS, 0L),
    TEN_OF_DIAMONDS(BlackjackRank.TEN, BlackjackSuit.DIAMONDS, 0L),
    JACK_OF_DIAMONDS(BlackjackRank.JACK, BlackjackSuit.DIAMONDS, 0L),
    QUEEN_OF_DIAMONDS(BlackjackRank.QUEEN, BlackjackSuit.DIAMONDS, 0L),
    KING_OF_DIAMONDS(BlackjackRank.KING, BlackjackSuit.DIAMONDS, 0L),

    ACE_OF_HEARTS(BlackjackRank.ACE, BlackjackSuit.HEARTS, 0L),
    TWO_OF_HEARTS(BlackjackRank.TWO, BlackjackSuit.HEARTS, 0L),
    THREE_OF_HEARTS(BlackjackRank.THREE, BlackjackSuit.HEARTS, 0L),
    FOUR_OF_HEARTS(BlackjackRank.FOUR, BlackjackSuit.HEARTS, 0L),
    FIVE_OF_HEARTS(BlackjackRank.FIVE, BlackjackSuit.HEARTS, 0L),
    SIX_OF_HEARTS(BlackjackRank.SIX, BlackjackSuit.HEARTS, 0L),
    SEVEN_OF_HEARTS(BlackjackRank.SEVEN, BlackjackSuit.HEARTS, 0L),
    EIGHT_OF_HEARTS(BlackjackRank.EIGHT, BlackjackSuit.HEARTS, 0L),
    NINE_OF_HEARTS(BlackjackRank.NINE, BlackjackSuit.HEARTS, 0L),
    TEN_OF_HEARTS(BlackjackRank.TEN, BlackjackSuit.HEARTS, 0L),
    JACK_OF_HEARTS(BlackjackRank.JACK, BlackjackSuit.HEARTS, 0L),
    QUEEN_OF_HEARTS(BlackjackRank.QUEEN, BlackjackSuit.HEARTS, 0L),
    KING_OF_HEARTS(BlackjackRank.KING, BlackjackSuit.HEARTS, 0L),

    ACE_OF_SPADES(BlackjackRank.ACE, BlackjackSuit.SPADES, 0L),
    TWO_OF_SPADES(BlackjackRank.TWO, BlackjackSuit.SPADES, 0L),
    THREE_OF_SPADES(BlackjackRank.THREE, BlackjackSuit.SPADES, 0L),
    FOUR_OF_SPADES(BlackjackRank.FOUR, BlackjackSuit.SPADES, 0L),
    FIVE_OF_SPADES(BlackjackRank.FIVE, BlackjackSuit.SPADES, 0L),
    SIX_OF_SPADES(BlackjackRank.SIX, BlackjackSuit.SPADES, 0L),
    SEVEN_OF_SPADES(BlackjackRank.SEVEN, BlackjackSuit.SPADES, 0L),
    EIGHT_OF_SPADES(BlackjackRank.EIGHT, BlackjackSuit.SPADES, 0L),
    NINE_OF_SPADES(BlackjackRank.NINE, BlackjackSuit.SPADES, 0L),
    TEN_OF_SPADES(BlackjackRank.TEN, BlackjackSuit.SPADES, 0L),
    JACK_OF_SPADES(BlackjackRank.JACK, BlackjackSuit.SPADES, 0L),
    QUEEN_OF_SPADES(BlackjackRank.QUEEN, BlackjackSuit.SPADES, 0L),
    KING_OF_SPADES(BlackjackRank.KING, BlackjackSuit.SPADES, 0L);

    public static final String BACK_EMOJI_NAME = "bj_back";
    public static final long BACK_EMOJI_ID = 0L;

    private final BlackjackRank rank;
    private final BlackjackSuit suit;
    private final String emojiName;
    private final long emojiId;

    BlackjackCard(BlackjackRank rank, BlackjackSuit suit, long emojiId) {
        this.rank = rank;
        this.suit = suit;
        this.emojiName = "bj_" + rank.emojiToken() + "_" + suit.emojiToken();
        this.emojiId = emojiId;
    }

    public BlackjackRank rank() {
        return rank;
    }

    public BlackjackSuit suit() {
        return suit;
    }

    public String emojiName() {
        return emojiName;
    }

    public long emojiId() {
        return emojiId;
    }

    public String display() {
        return emojiId > 0
                ? String.format("<:%s:%d>", emojiName, emojiId)
                : "`" + rank.label() + suit.label() + "`";
    }

    public static String backDisplay() {
        return BACK_EMOJI_ID > 0
                ? String.format("<:%s:%d>", BACK_EMOJI_NAME, BACK_EMOJI_ID)
                : "`??`";
    }
}
