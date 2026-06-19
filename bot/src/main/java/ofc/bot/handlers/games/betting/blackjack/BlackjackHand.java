package ofc.bot.handlers.games.betting.blackjack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class BlackjackHand {
    private final List<BlackjackCard> cards;
    private final boolean naturalEligible;
    private int stake;
    private boolean stood;
    private boolean doubled;

    public BlackjackHand(int stake, boolean naturalEligible) {
        this.cards = new ArrayList<>();
        this.stake = stake;
        this.naturalEligible = naturalEligible;
    }

    private BlackjackHand(List<BlackjackCard> cards, int stake, boolean naturalEligible) {
        this.cards = new ArrayList<>(cards);
        this.stake = stake;
        this.naturalEligible = naturalEligible;
    }

    public void add(BlackjackCard card) {
        cards.add(card);
    }

    public void addStake(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Stake amount must be positive.");
        }
        stake += amount;
        doubled = true;
    }

    public void stand() {
        stood = true;
    }

    public List<BlackjackCard> cards() {
        return List.copyOf(cards);
    }

    public int stake() {
        return stake;
    }

    public boolean stood() {
        return stood;
    }

    public boolean doubled() {
        return doubled;
    }

    public boolean finished() {
        return stood || value().busted();
    }

    public boolean isNatural() {
        return naturalEligible && cards.size() == 2 && value().total() == 21;
    }

    public boolean hasEqualRanks() {
        return cards.size() == 2 && cards.get(0).rank() == cards.get(1).rank();
    }

    public BlackjackHandValue value() {
        int total = 0;
        int aces = 0;
        for (BlackjackCard card : cards) {
            total += card.rank().value();
            if (card.rank() == BlackjackRank.ACE) {
                aces++;
            }
        }

        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return new BlackjackHandValue(total, aces > 0);
    }

    public String displayCards() {
        return cards.stream()
                .map(BlackjackCard::display)
                .collect(Collectors.joining(" "));
    }

    BlackjackHand singleCardCopy(int cardIndex) {
        return new BlackjackHand(List.of(cards.get(cardIndex)), stake, false);
    }
}
