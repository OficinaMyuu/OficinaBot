package ofc.bot.handlers.games.betting.blackjack;

public enum BlackjackCard {
    ACE_OF_CLUBS(BlackjackRank.ACE, BlackjackSuit.CLUBS, 1517710296876056740L),
    TWO_OF_CLUBS(BlackjackRank.TWO, BlackjackSuit.CLUBS, 1517710298075762790L),
    THREE_OF_CLUBS(BlackjackRank.THREE, BlackjackSuit.CLUBS, 1517710299397099651L),
    FOUR_OF_CLUBS(BlackjackRank.FOUR, BlackjackSuit.CLUBS, 1517710301141799064L),
    FIVE_OF_CLUBS(BlackjackRank.FIVE, BlackjackSuit.CLUBS, 1517710302743887922L),
    SIX_OF_CLUBS(BlackjackRank.SIX, BlackjackSuit.CLUBS, 1517710303901646968L),
    SEVEN_OF_CLUBS(BlackjackRank.SEVEN, BlackjackSuit.CLUBS, 1517710305432567890L),
    EIGHT_OF_CLUBS(BlackjackRank.EIGHT, BlackjackSuit.CLUBS, 1517710306707640482L),
    NINE_OF_CLUBS(BlackjackRank.NINE, BlackjackSuit.CLUBS, 1517710307810742333L),
    TEN_OF_CLUBS(BlackjackRank.TEN, BlackjackSuit.CLUBS, 1517710309568155679L),
    JACK_OF_CLUBS(BlackjackRank.JACK, BlackjackSuit.CLUBS, 1517710310730104862L),
    QUEEN_OF_CLUBS(BlackjackRank.QUEEN, BlackjackSuit.CLUBS, 1517710312323682354L),
    KING_OF_CLUBS(BlackjackRank.KING, BlackjackSuit.CLUBS, 1517710313850671214L),

    ACE_OF_DIAMONDS(BlackjackRank.ACE, BlackjackSuit.DIAMONDS, 1517710315658154114L),
    TWO_OF_DIAMONDS(BlackjackRank.TWO, BlackjackSuit.DIAMONDS, 1517710317130354829L),
    THREE_OF_DIAMONDS(BlackjackRank.THREE, BlackjackSuit.DIAMONDS, 1517710318539898901L),
    FOUR_OF_DIAMONDS(BlackjackRank.FOUR, BlackjackSuit.DIAMONDS, 1517710320066363392L),
    FIVE_OF_DIAMONDS(BlackjackRank.FIVE, BlackjackSuit.DIAMONDS, 1517710321710665818L),
    SIX_OF_DIAMONDS(BlackjackRank.SIX, BlackjackSuit.DIAMONDS, 1517710322914557973L),
    SEVEN_OF_DIAMONDS(BlackjackRank.SEVEN, BlackjackSuit.DIAMONDS, 1517710323983843470L),
    EIGHT_OF_DIAMONDS(BlackjackRank.EIGHT, BlackjackSuit.DIAMONDS, 1517710325158383636L),
    NINE_OF_DIAMONDS(BlackjackRank.NINE, BlackjackSuit.DIAMONDS, 1517710326391378060L),
    TEN_OF_DIAMONDS(BlackjackRank.TEN, BlackjackSuit.DIAMONDS, 1517710327544807464L),
    JACK_OF_DIAMONDS(BlackjackRank.JACK, BlackjackSuit.DIAMONDS, 1517710329428049921L),
    QUEEN_OF_DIAMONDS(BlackjackRank.QUEEN, BlackjackSuit.DIAMONDS, 1517710330669695046L),
    KING_OF_DIAMONDS(BlackjackRank.KING, BlackjackSuit.DIAMONDS, 1517710332196556830L),

    ACE_OF_HEARTS(BlackjackRank.ACE, BlackjackSuit.HEARTS, 1517710333098070099L),
    TWO_OF_HEARTS(BlackjackRank.TWO, BlackjackSuit.HEARTS, 1517710334805278860L),
    THREE_OF_HEARTS(BlackjackRank.THREE, BlackjackSuit.HEARTS, 1517710336076152842L),
    FOUR_OF_HEARTS(BlackjackRank.FOUR, BlackjackSuit.HEARTS, 1517710337992949893L),
    FIVE_OF_HEARTS(BlackjackRank.FIVE, BlackjackSuit.HEARTS, 1517710339209166870L),
    SIX_OF_HEARTS(BlackjackRank.SIX, BlackjackSuit.HEARTS, 1517710340744413274L),
    SEVEN_OF_HEARTS(BlackjackRank.SEVEN, BlackjackSuit.HEARTS, 1517710342246105228L),
    EIGHT_OF_HEARTS(BlackjackRank.EIGHT, BlackjackSuit.HEARTS, 1517710343340691587L),
    NINE_OF_HEARTS(BlackjackRank.NINE, BlackjackSuit.HEARTS, 1517710344909492318L),
    TEN_OF_HEARTS(BlackjackRank.TEN, BlackjackSuit.HEARTS, 1517710346167652412L),
    JACK_OF_HEARTS(BlackjackRank.JACK, BlackjackSuit.HEARTS, 1517710347375476967L),
    QUEEN_OF_HEARTS(BlackjackRank.QUEEN, BlackjackSuit.HEARTS, 1517710348528914463L),
    KING_OF_HEARTS(BlackjackRank.KING, BlackjackSuit.HEARTS, 1517710349694926859L),

    ACE_OF_SPADES(BlackjackRank.ACE, BlackjackSuit.SPADES, 1517710350890565783L),
    TWO_OF_SPADES(BlackjackRank.TWO, BlackjackSuit.SPADES, 1517710352371027988L),
    THREE_OF_SPADES(BlackjackRank.THREE, BlackjackSuit.SPADES, 1517710353524326550L),
    FOUR_OF_SPADES(BlackjackRank.FOUR, BlackjackSuit.SPADES, 1517710354623500388L),
    FIVE_OF_SPADES(BlackjackRank.FIVE, BlackjackSuit.SPADES, 1517710355822809141L),
    SIX_OF_SPADES(BlackjackRank.SIX, BlackjackSuit.SPADES, 1517710356745683097L),
    SEVEN_OF_SPADES(BlackjackRank.SEVEN, BlackjackSuit.SPADES, 1517710358704553984L),
    EIGHT_OF_SPADES(BlackjackRank.EIGHT, BlackjackSuit.SPADES, 1517710359954198679L),
    NINE_OF_SPADES(BlackjackRank.NINE, BlackjackSuit.SPADES, 1517710361397301249L),
    TEN_OF_SPADES(BlackjackRank.TEN, BlackjackSuit.SPADES, 1517710362978422906L),
    JACK_OF_SPADES(BlackjackRank.JACK, BlackjackSuit.SPADES, 1517710364362543184L),
    QUEEN_OF_SPADES(BlackjackRank.QUEEN, BlackjackSuit.SPADES, 1517710365964894260L),
    KING_OF_SPADES(BlackjackRank.KING, BlackjackSuit.SPADES, 1517710367315464242L);

    public static final String BACK_EMOJI_NAME = "bj_back";
    public static final long BACK_EMOJI_ID = 1517711868003745792L;

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
        return String.format("<:%s:%d>", BACK_EMOJI_NAME, BACK_EMOJI_ID);
    }
}
