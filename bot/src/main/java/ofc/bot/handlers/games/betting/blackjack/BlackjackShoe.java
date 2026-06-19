package ofc.bot.handlers.games.betting.blackjack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlackjackShoe {
    public static final int DECK_COUNT = 3;
    public static final int INITIAL_SIZE = DECK_COUNT * BlackjackCard.values().length;

    private final List<BlackjackCard> cards;

    public BlackjackShoe() {
        this(createShuffledCards());
    }

    BlackjackShoe(List<BlackjackCard> cards) {
        if (cards.isEmpty()) {
            throw new IllegalArgumentException("Blackjack shoe cannot be empty.");
        }
        this.cards = new ArrayList<>(cards);
    }

    public BlackjackCard draw() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Blackjack shoe is empty.");
        }
        return cards.removeFirst();
    }

    public int remaining() {
        return cards.size();
    }

    static BlackjackShoe fixed(BlackjackCard... cards) {
        return new BlackjackShoe(List.of(cards));
    }

    private static List<BlackjackCard> createShuffledCards() {
        List<BlackjackCard> cards = new ArrayList<>(INITIAL_SIZE);
        for (int i = 0; i < DECK_COUNT; i++) {
            Collections.addAll(cards, BlackjackCard.values());
        }
        Collections.shuffle(cards);
        return cards;
    }
}
