package ofc.bot.handlers.games.betting.blackjack;

import java.util.ArrayList;
import java.util.List;

public final class BlackjackRound {
    private final BlackjackShoe shoe;
    private final BlackjackHand dealer;
    private final List<BlackjackHand> playerHands;
    private int activeHandIndex;
    private boolean splitUsed;
    private boolean settled;

    public BlackjackRound(int stake) {
        this(stake, new BlackjackShoe());
    }

    BlackjackRound(int stake, BlackjackShoe shoe) {
        this.shoe = shoe;
        this.dealer = new BlackjackHand(0, true);
        this.playerHands = new ArrayList<>();
        this.playerHands.add(new BlackjackHand(stake, true));
        this.activeHandIndex = 0;

        activeHand().add(shoe.draw());
        dealer.add(shoe.draw());
        activeHand().add(shoe.draw());
        dealer.add(shoe.draw());

        if (activeHand().isNatural() || dealer.isNatural()) {
            settle(false);
        }
    }

    public BlackjackHand dealer() {
        return dealer;
    }

    public List<BlackjackHand> playerHands() {
        return List.copyOf(playerHands);
    }

    public BlackjackHand activeHand() {
        if (settled) {
            return playerHands.getLast();
        }
        return playerHands.get(activeHandIndex);
    }

    public int activeHandIndex() {
        return activeHandIndex;
    }

    public boolean isSettled() {
        return settled;
    }

    public boolean splitUsed() {
        return splitUsed;
    }

    public int cardsRemaining() {
        return shoe.remaining();
    }

    public int totalStake() {
        return playerHands.stream().mapToInt(BlackjackHand::stake).sum();
    }

    public int requiredDoubleStake() {
        return activeHand().stake();
    }

    public int requiredSplitStake() {
        return activeHand().stake();
    }

    public boolean canHit() {
        return !settled && !activeHand().finished();
    }

    public boolean canStand() {
        return !settled && !activeHand().finished();
    }

    public boolean canDoubleDown() {
        return canHit() && activeHand().cards().size() == 2 && !activeHand().doubled();
    }

    public boolean canSplit() {
        return canHit()
                && !splitUsed
                && playerHands.size() == 1
                && activeHand().hasEqualRanks();
    }

    public void hit() {
        if (!canHit()) {
            throw new IllegalStateException("Current Blackjack hand cannot hit.");
        }
        activeHand().add(shoe.draw());
        advanceIfNeeded();
    }

    public void stand() {
        if (!canStand()) {
            throw new IllegalStateException("Current Blackjack hand cannot stand.");
        }
        activeHand().stand();
        advanceIfNeeded();
    }

    public void standAll() {
        if (settled) return;

        for (BlackjackHand hand : playerHands) {
            hand.stand();
        }
        settle(true);
    }

    public void doubleDown() {
        if (!canDoubleDown()) {
            throw new IllegalStateException("Current Blackjack hand cannot double down.");
        }
        BlackjackHand hand = activeHand();
        hand.addStake(hand.stake());
        hand.add(shoe.draw());
        hand.stand();
        advanceIfNeeded();
    }

    public void split() {
        if (!canSplit()) {
            throw new IllegalStateException("Current Blackjack hand cannot split.");
        }
        BlackjackHand current = activeHand();
        BlackjackHand first = current.singleCardCopy(0);
        BlackjackHand second = current.singleCardCopy(1);

        first.add(shoe.draw());
        second.add(shoe.draw());
        playerHands.clear();
        playerHands.add(first);
        playerHands.add(second);
        activeHandIndex = 0;
        splitUsed = true;
        advanceIfNeeded();
    }

    public List<BlackjackResolvedHand> resolvedHands() {
        if (!settled) {
            return List.of();
        }
        return playerHands.stream()
                .map(hand -> new BlackjackResolvedHand(hand, resolve(hand)))
                .toList();
    }

    private void advanceIfNeeded() {
        while (activeHandIndex < playerHands.size() && playerHands.get(activeHandIndex).finished()) {
            activeHandIndex++;
        }
        if (activeHandIndex >= playerHands.size()) {
            settle(true);
        }
    }

    private void settle(boolean playDealer) {
        if (settled) return;

        boolean hasLiveHand = playerHands.stream().anyMatch(hand -> !hand.value().busted());
        if (playDealer && hasLiveHand && !dealer.isNatural()) {
            while (dealer.value().total() < 17) {
                dealer.add(shoe.draw());
            }
        }
        settled = true;
        activeHandIndex = Math.max(playerHands.size() - 1, 0);
    }

    private BlackjackOutcome resolve(BlackjackHand hand) {
        if (hand.value().busted()) return BlackjackOutcome.LOSS;
        if (hand.isNatural() && dealer.isNatural()) return BlackjackOutcome.PUSH;
        if (hand.isNatural()) return BlackjackOutcome.WIN;
        if (dealer.isNatural()) return BlackjackOutcome.LOSS;
        if (dealer.value().busted()) return BlackjackOutcome.WIN;

        int playerTotal = hand.value().total();
        int dealerTotal = dealer.value().total();
        if (playerTotal > dealerTotal) return BlackjackOutcome.WIN;
        if (playerTotal < dealerTotal) return BlackjackOutcome.LOSS;
        return BlackjackOutcome.PUSH;
    }
}
